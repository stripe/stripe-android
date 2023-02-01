package com.stripe.android.paymentsheet.flowcontroller

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.PaymentIntent
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.ConfirmationHandler
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.DaggerFlowControllerComponent
import com.stripe.android.paymentsheet.injection.FlowControllerComponent
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.validate
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import com.stripe.android.uicore.address.AddressRepository
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.security.InvalidParameterException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class DefaultFlowController @Inject internal constructor(
    // Properties provided through FlowControllerComponent.Builder
    private val lifecycleScope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    private val statusBarColor: () -> Int?,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback,
    activityResultCaller: ActivityResultCaller,
    @InjectorKey private val injectorKey: String,
    // Properties provided through injection
    private val paymentSheetLoader: PaymentSheetLoader,
    private val eventReporter: EventReporter,
    private val viewModel: FlowControllerViewModel,
    // even though unused this forces Dagger to initialize it here.
    private val lpmResourceRepository: ResourceRepository<LpmRepository>,
    private val addressResourceRepository: ResourceRepository<AddressRepository>,
    /**
     * [PaymentConfiguration] is [Lazy] because the client might set publishableKey and
     * stripeAccountId after creating a [DefaultFlowController].
     */
    @UIContext private val uiContext: CoroutineContext,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
    private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
    private val linkLauncher: LinkPaymentLauncher,
    private val confirmationHandler: ConfirmationHandler,
) : PaymentSheet.FlowController, NonFallbackInjector {
    private val paymentOptionActivityLauncher: ActivityResultLauncher<PaymentOptionContract.Args>
    private val googlePayActivityLauncher:
        ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>

    /**
     * [FlowControllerComponent] is hold to inject into [Activity]s and created
     * after [DefaultFlowController].
     */
    lateinit var flowControllerComponent: FlowControllerComponent

    private val resourceRepositories = listOf(lpmResourceRepository, addressResourceRepository)

    override var shippingDetails: AddressDetails?
        get() = viewModel.state?.config?.shippingDetails
        set(value) {
            viewModel.state = viewModel.state?.copy(
                config = viewModel.state?.config?.copy(
                    shippingDetails = value
                )
            )
        }

    override fun inject(injectable: Injectable<*>) {
        when (injectable) {
            is PaymentOptionsViewModel.Factory -> {
                flowControllerComponent.inject(injectable)
            }
            is FormViewModel.Factory -> {
                flowControllerComponent.inject(injectable)
            }
            else -> {
                throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    confirmationHandler.registerFromActivity(activityResultCaller)

                    linkLauncher.register(
                        activityResultCaller = activityResultCaller,
                        callback = ::onLinkActivityResult,
                    )
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    confirmationHandler.unregisterFromActivity()
                    linkLauncher.unregister()
                }
            }
        )

        paymentOptionActivityLauncher = activityResultCaller.registerForActivityResult(
            PaymentOptionContract(),
            ::onPaymentOptionResult
        )

        googlePayActivityLauncher = activityResultCaller.registerForActivityResult(
            GooglePayPaymentMethodLauncherContract(),
            ::onGooglePayResult
        )
    }

    override fun configureWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        configureInternal(
            PaymentIntentClientSecret(paymentIntentClientSecret),
            configuration,
            callback
        )
    }

    override fun configureWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        configureInternal(
            SetupIntentClientSecret(setupIntentClientSecret),
            configuration,
            callback
        )
    }

    private fun configureInternal(
        clientSecret: ClientSecret,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        try {
            configuration?.validate()
            clientSecret.validate()
        } catch (e: InvalidParameterException) {
            callback.onConfigured(success = false, e)
            return
        }

        lifecycleScope.launch {
            val result = paymentSheetLoader.load(
                clientSecret,
                configuration
            )

            // Wait until all required resources are loaded before completing initialization.
            resourceRepositories.forEach { it.waitUntilLoaded() }

            if (isActive) {
                dispatchResult(result, callback)
            } else {
                callback.onConfigured(false, null)
            }
        }
    }

    override fun getPaymentOption(): PaymentOption? {
        return viewModel.paymentSelection?.let {
            paymentOptionFactory.create(it)
        }
    }

    override fun presentPaymentOptions() {
        val state = runCatching {
            requireNotNull(viewModel.state)
        }.getOrElse {
            error(
                "FlowController must be successfully initialized using " +
                    "configureWithPaymentIntent() or configureWithSetupIntent() " +
                    "before calling presentPaymentOptions()"
            )
        }

        paymentOptionActivityLauncher.launch(
            PaymentOptionContract.Args(
                state = state.copy(
                    newPaymentSelection = viewModel.paymentSelection as? PaymentSelection.New,
                ),
                statusBarColor = statusBarColor(),
                injectorKey = injectorKey,
                enableLogging = enableLogging,
                productUsage = productUsage,
            )
        )
    }

    override fun confirm() {
        val state = runCatching {
            requireNotNull(viewModel.state)
        }.getOrElse {
            error(
                "FlowController must be successfully initialized using " +
                    "configureWithPaymentIntent() or configureWithSetupIntent() " +
                    "before calling confirm()"
            )
        }

        when (val paymentSelection = viewModel.paymentSelection) {
            PaymentSelection.GooglePay -> launchGooglePay(state)
            PaymentSelection.Link,
            is PaymentSelection.New.LinkInline -> confirmLink(paymentSelection, state)
            else -> confirmPaymentSelection(paymentSelection, state)
        }
    }

    @VisibleForTesting
    fun confirmPaymentSelection(
        paymentSelection: PaymentSelection?,
        state: PaymentSheetState.Full,
    ) {
        val selection = paymentSelection ?: return

        lifecycleScope.launch {
            val paymentResult = confirmationHandler.confirm(
                paymentSelection = selection,
                clientSecret = state.clientSecret,
                shipping = state.config?.shippingDetails?.toConfirmPaymentIntentShipping(),
            ).getOrNull()

            if (paymentResult != null) {
                onPaymentResult(paymentResult, selection)
            }
        }
    }

    internal fun onGooglePayResult(
        googlePayResult: GooglePayPaymentMethodLauncher.Result
    ) {
        when (googlePayResult) {
            is GooglePayPaymentMethodLauncher.Result.Completed -> {
                runCatching {
                    requireNotNull(viewModel.state)
                }.fold(
                    onSuccess = { state ->
                        val paymentSelection = PaymentSelection.Saved(
                            googlePayResult.paymentMethod,
                            isGooglePay = true
                        )
                        viewModel.paymentSelection = paymentSelection
                        confirmPaymentSelection(
                            paymentSelection,
                            state
                        )
                    },
                    onFailure = {
                        eventReporter.onPaymentFailure(
                            PaymentSelection.GooglePay,
                            viewModel.state?.stripeIntent?.currency
                        )
                        paymentResultCallback.onPaymentSheetResult(
                            PaymentSheetResult.Failed(it)
                        )
                    }
                )
            }
            is GooglePayPaymentMethodLauncher.Result.Failed -> {
                eventReporter.onPaymentFailure(
                    PaymentSelection.GooglePay,
                    viewModel.state?.stripeIntent?.currency
                )
                paymentResultCallback.onPaymentSheetResult(
                    PaymentSheetResult.Failed(
                        GooglePayException(
                            googlePayResult.error
                        )
                    )
                )
            }
            is GooglePayPaymentMethodLauncher.Result.Canceled -> {
                // don't log cancellations as failures
                paymentResultCallback.onPaymentSheetResult(PaymentSheetResult.Canceled)
            }
        }
    }

    private fun onLinkActivityResult(result: LinkActivityResult) {
        onPaymentResult(result.convertToPaymentResult(), PaymentSelection.Link)
    }

    private suspend fun dispatchResult(
        result: PaymentSheetLoader.Result,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) = withContext(uiContext) {
        when (result) {
            is PaymentSheetLoader.Result.Success -> {
                onInitSuccess(result.state, callback)
            }
            is PaymentSheetLoader.Result.Failure -> {
                callback.onConfigured(false, result.throwable)
            }
        }
    }

    private fun onInitSuccess(
        state: PaymentSheetState.Full,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        eventReporter.onInit(state.config)

        viewModel.paymentSelection = state.initialPaymentSelection
        viewModel.state = state

        callback.onConfigured(true, null)
    }

    @JvmSynthetic
    internal fun onPaymentOptionResult(
        paymentOptionResult: PaymentOptionResult?
    ) {
        paymentOptionResult?.paymentMethods?.let {
            viewModel.state = viewModel.state?.copy(customerPaymentMethods = it)
        }
        when (paymentOptionResult) {
            is PaymentOptionResult.Succeeded -> {
                val paymentSelection = paymentOptionResult.paymentSelection
                viewModel.paymentSelection = paymentSelection
                paymentOptionCallback.onPaymentOption(
                    paymentOptionFactory.create(
                        paymentSelection
                    )
                )
            }
            is PaymentOptionResult.Failed, is PaymentOptionResult.Canceled -> {
                paymentOptionCallback.onPaymentOption(
                    viewModel.paymentSelection?.let {
                        paymentOptionFactory.create(it)
                    }
                )
            }
            else -> {
                viewModel.paymentSelection = null
                paymentOptionCallback.onPaymentOption(null)
            }
        }
    }

    internal fun onPaymentResult(paymentResult: PaymentResult, selection: PaymentSelection) {
        logPaymentResult(paymentResult, selection)
        lifecycleScope.launch {
            paymentResultCallback.onPaymentSheetResult(
                paymentResult.convertToPaymentSheetResult()
            )
        }
    }

    private fun logPaymentResult(paymentResult: PaymentResult?, selection: PaymentSelection) {
        when (paymentResult) {
            is PaymentResult.Completed -> {
                if ((selection as? PaymentSelection.Saved)?.isGooglePay == true) {
                    // Google Pay is treated as a saved PM after confirmation
                    eventReporter.onPaymentSuccess(
                        PaymentSelection.GooglePay,
                        viewModel.state?.stripeIntent?.currency
                    )
                } else {
                    eventReporter.onPaymentSuccess(
                        selection,
                        viewModel.state?.stripeIntent?.currency
                    )
                }
            }
            is PaymentResult.Failed -> {
                eventReporter.onPaymentFailure(
                    selection,
                    viewModel.state?.stripeIntent?.currency
                )
            }
            else -> {}
        }
    }

    private fun confirmLink(
        paymentSelection: PaymentSelection,
        state: PaymentSheetState.Full
    ) {
        val linkConfig = requireNotNull(state.linkState).configuration

        lifecycleScope.launch {
            val accountStatus = linkLauncher.getAccountStatusFlow(linkConfig).first()

            val linkInline = (paymentSelection as? PaymentSelection.New.LinkInline)?.takeIf {
                accountStatus == AccountStatus.Verified
            }

            if (linkInline != null) {
                // If a returning user is paying with a new card inline, launch Link
                linkLauncher.present(
                    configuration = linkConfig,
                    prefilledNewCardParams = linkInline.linkPaymentDetails.originalParams,
                )
            } else if (paymentSelection is PaymentSelection.Link) {
                // User selected Link as the payment method, not inline
                linkLauncher.present(linkConfig)
            } else {
                // New user paying inline, complete without launching Link
                confirmPaymentSelection(paymentSelection, state)
            }
        }
    }

    private fun launchGooglePay(state: PaymentSheetState.Full) {
        // state.config.googlePay is guaranteed not to be null or GooglePay would be disabled
        val config = requireNotNull(state.config)
        val googlePayConfig = requireNotNull(config.googlePay)
        val googlePayPaymentLauncherConfig = GooglePayPaymentMethodLauncher.Config(
            environment = when (googlePayConfig.environment) {
                PaymentSheet.GooglePayConfiguration.Environment.Production ->
                    GooglePayEnvironment.Production
                else ->
                    GooglePayEnvironment.Test
            },
            merchantCountryCode = googlePayConfig.countryCode,
            merchantName = config.merchantDisplayName
        )

        googlePayPaymentMethodLauncherFactory.create(
            lifecycleScope = lifecycleScope,
            config = googlePayPaymentLauncherConfig,
            readyCallback = {},
            activityResultLauncher = googlePayActivityLauncher,
            skipReadyCheck = true
        ).present(
            currencyCode = (state.stripeIntent as? PaymentIntent)?.currency
                ?: googlePayConfig.currencyCode.orEmpty(),
            amount = (state.stripeIntent as? PaymentIntent)?.amount?.toInt() ?: 0,
            transactionId = state.stripeIntent.id
        )
    }

    private fun PaymentResult.convertToPaymentSheetResult() = when (this) {
        is PaymentResult.Completed -> PaymentSheetResult.Completed
        is PaymentResult.Canceled -> PaymentSheetResult.Canceled
        is PaymentResult.Failed -> PaymentSheetResult.Failed(throwable)
    }

    private fun LinkActivityResult.convertToPaymentResult() = when (this) {
        is LinkActivityResult.Completed -> PaymentResult.Completed
        is LinkActivityResult.Canceled -> PaymentResult.Canceled
        is LinkActivityResult.Failed -> PaymentResult.Failed(error)
    }

    class GooglePayException(
        val throwable: Throwable
    ) : Exception(throwable)

    @Parcelize
    data class Args(
        val clientSecret: String,
        val config: PaymentSheet.Configuration?
    ) : Parcelable

    companion object {
        fun getInstance(
            appContext: Context,
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            activityResultCaller: ActivityResultCaller,
            statusBarColor: () -> Int?,
            paymentOptionCallback: PaymentOptionCallback,
            paymentResultCallback: PaymentSheetResultCallback
        ): PaymentSheet.FlowController {
            val injectorKey =
                WeakMapInjectorRegistry.nextKey(
                    requireNotNull(PaymentSheet.FlowController::class.simpleName)
                )
            val flowControllerComponent = DaggerFlowControllerComponent.builder()
                .appContext(appContext)
                .viewModelStoreOwner(viewModelStoreOwner)
                .lifeCycleOwner(lifecycleOwner)
                .activityResultCaller(activityResultCaller)
                .statusBarColor(statusBarColor)
                .paymentOptionCallback(paymentOptionCallback)
                .paymentResultCallback(paymentResultCallback)
                .injectorKey(injectorKey)
                .build()
            val flowController = flowControllerComponent.flowController
            flowController.flowControllerComponent = flowControllerComponent
            WeakMapInjectorRegistry.register(flowController, injectorKey)
            return flowController
        }
    }
}
