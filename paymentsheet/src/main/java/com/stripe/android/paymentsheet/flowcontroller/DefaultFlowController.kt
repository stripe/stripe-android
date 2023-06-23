package com.stripe.android.paymentsheet.flowcontroller

import android.app.Activity
import android.os.Parcelable
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.paymentsheet.IntentConfirmationInterceptor
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
import com.stripe.android.paymentsheet.extensions.registerPollingAuthenticator
import com.stripe.android.paymentsheet.extensions.unregisterPollingAuthenticator
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.intercept
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.utils.AnimationConstants
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

@FlowControllerScope
internal class DefaultFlowController @Inject internal constructor(
    // Properties provided through FlowControllerComponent.Builder
    private val viewModelScope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    private val statusBarColor: () -> Int?,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    @InjectorKey private val injectorKey: String,
    // Properties provided through injection
    private val eventReporter: EventReporter,
    private val viewModel: FlowControllerViewModel,
    private val paymentLauncherFactory: StripePaymentLauncherAssistedFactory,
    /**
     * [PaymentConfiguration] is [Lazy] because the client might set publishableKey and
     * stripeAccountId after creating a [DefaultFlowController].
     */
    private val lazyPaymentConfiguration: Provider<PaymentConfiguration>,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
    private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
    private val linkLauncher: LinkPaymentLauncher,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val configurationHandler: FlowControllerConfigurationHandler,
    private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
) : PaymentSheet.FlowController, NonFallbackInjector {
    private val paymentOptionActivityLauncher: ActivityResultLauncher<PaymentOptionContract.Args>
    private val googlePayActivityLauncher:
        ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>

    /**
     * [FlowControllerComponent] is hold to inject into [Activity]s and created
     * after [DefaultFlowController].
     */
    lateinit var flowControllerComponent: FlowControllerComponent

    private var paymentLauncher: StripePaymentLauncher? = null

    private val initializationMode: PaymentSheet.InitializationMode?
        get() = viewModel.previousConfigureRequest?.initializationMode

    private val isDecoupling: Boolean
        get() = initializationMode is PaymentSheet.InitializationMode.DeferredIntent

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
                flowControllerComponent.stateComponent.inject(injectable)
            }
            is FormViewModel.Factory -> {
                flowControllerComponent.stateComponent.inject(injectable)
            }
            else -> {
                throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
            }
        }
    }

    init {
        val paymentLauncherActivityResultLauncher = activityResultRegistryOwner.register(
            PaymentLauncherContract(),
            ::onPaymentResult
        )

        paymentOptionActivityLauncher = activityResultRegistryOwner.register(
            PaymentOptionContract(),
            ::onPaymentOptionResult
        )

        googlePayActivityLauncher = activityResultRegistryOwner.register(
            GooglePayPaymentMethodLauncherContractV2(),
            ::onGooglePayResult
        )

        val activityResultLaunchers = setOf(
            paymentLauncherActivityResultLauncher,
            paymentOptionActivityLauncher,
            googlePayActivityLauncher,
        )

        linkLauncher.register(
            activityResultRegistry = activityResultRegistryOwner.activityResultRegistry,
            callback = ::onLinkActivityResult,
        )

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    paymentLauncher = paymentLauncherFactory.create(
                        publishableKey = { lazyPaymentConfiguration.get().publishableKey },
                        stripeAccountId = { lazyPaymentConfiguration.get().stripeAccountId },
                        statusBarColor = statusBarColor(),
                        hostActivityLauncher = paymentLauncherActivityResultLauncher,
                    ).also {
                        it.registerPollingAuthenticator()
                    }
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    activityResultLaunchers.forEach { it.unregister() }
                    paymentLauncher?.unregisterPollingAuthenticator()
                    paymentLauncher = null
                    linkLauncher.unregister()
                }
            }
        )
    }

    override fun configureWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        configure(
            mode = PaymentSheet.InitializationMode.PaymentIntent(paymentIntentClientSecret),
            configuration = configuration,
            callback = callback,
        )
    }

    override fun configureWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        configure(
            mode = PaymentSheet.InitializationMode.SetupIntent(setupIntentClientSecret),
            configuration = configuration,
            callback = callback,
        )
    }

    @ExperimentalPaymentSheetDecouplingApi
    override fun configureWithIntentConfiguration(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        configure(
            mode = PaymentSheet.InitializationMode.DeferredIntent(intentConfiguration),
            configuration = configuration,
            callback = callback,
        )
    }

    private fun configure(
        mode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        configurationHandler.configure(
            scope = viewModelScope,
            initializationMode = mode,
            configuration = configuration,
            callback = callback,
        )
    }

    override fun getPaymentOption(): PaymentOption? {
        return viewModel.paymentSelection?.let {
            paymentOptionFactory.create(it)
        }
    }

    override fun presentPaymentOptions() {
        val state = viewModel.state ?: error(
            "FlowController must be successfully initialized " +
                "using configureWithPaymentIntent(), configureWithSetupIntent() or " +
                "configureWithIntentConfiguration() before calling presentPaymentOptions()."
        )

        if (!configurationHandler.isConfigured) {
            // For now, we fail silently in these situations. In the future, we should either log
            // or emit an event in a to-be-added event listener.
            return
        }

        val args = PaymentOptionContract.Args(
            state = state.copy(paymentSelection = viewModel.paymentSelection),
            statusBarColor = statusBarColor(),
            injectorKey = injectorKey,
            enableLogging = enableLogging,
            productUsage = productUsage,
        )

        val options = ActivityOptionsCompat.makeCustomAnimation(
            viewModel.getApplication(),
            AnimationConstants.FADE_IN,
            AnimationConstants.FADE_OUT,
        )

        paymentOptionActivityLauncher.launch(args, options)
    }

    override fun confirm() {
        val state = viewModel.state ?: error(
            "FlowController must be successfully initialized " +
                "using configureWithPaymentIntent(), configureWithSetupIntent() or " +
                "configureWithIntentConfiguration() before calling confirm()."
        )

        if (!configurationHandler.isConfigured) {
            val error = IllegalStateException(
                "FlowController.confirm() can only be called if the most recent call " +
                    "to configureWithPaymentIntent(), configureWithSetupIntent() or " +
                    "configureWithIntentConfiguration() has completed successfully."
            )
            onPaymentResult(PaymentResult.Failed(error))
            return
        }

        when (val paymentSelection = viewModel.paymentSelection) {
            is PaymentSelection.GooglePay -> launchGooglePay(state)
            is PaymentSelection.Link,
            is PaymentSelection.New.LinkInline -> confirmLink(paymentSelection, state)
            is PaymentSelection.New,
            is PaymentSelection.Saved,
            null -> confirmPaymentSelection(paymentSelection, state)
        }
    }

    @VisibleForTesting
    fun confirmPaymentSelection(
        paymentSelection: PaymentSelection?,
        state: PaymentSheetState.Full,
    ) {
        viewModelScope.launch {
            val stripeIntent = requireNotNull(state.stripeIntent)

            val nextStep = intentConfirmationInterceptor.intercept(
                initializationMode = initializationMode!!,
                paymentSelection = paymentSelection,
                shippingValues = state.config?.shippingDetails?.toConfirmPaymentIntentShipping(),
            )

            viewModel.deferredIntentConfirmationType = nextStep.deferredIntentConfirmationType

            when (nextStep) {
                is IntentConfirmationInterceptor.NextStep.HandleNextAction -> {
                    handleNextAction(
                        clientSecret = nextStep.clientSecret,
                        stripeIntent = stripeIntent,
                    )
                }
                is IntentConfirmationInterceptor.NextStep.Confirm -> {
                    confirmStripeIntent(nextStep.confirmParams)
                }
                is IntentConfirmationInterceptor.NextStep.Fail -> {
                    onPaymentResult(
                        PaymentResult.Failed(
                            nextStep.cause
                        )
                    )
                }
                is IntentConfirmationInterceptor.NextStep.Complete -> {
                    onPaymentResult(PaymentResult.Completed)
                }
            }
        }
    }

    private fun confirmStripeIntent(confirmStripeIntentParams: ConfirmStripeIntentParams) {
        runCatching {
            requireNotNull(paymentLauncher)
        }.fold(
            onSuccess = {
                when (confirmStripeIntentParams) {
                    is ConfirmPaymentIntentParams -> {
                        it.confirm(confirmStripeIntentParams)
                    }
                    is ConfirmSetupIntentParams -> {
                        it.confirm(confirmStripeIntentParams)
                    }
                }
            },
            onFailure = ::error
        )
    }

    private fun handleNextAction(
        clientSecret: String,
        stripeIntent: StripeIntent,
    ) {
        runCatching {
            requireNotNull(paymentLauncher)
        }.fold(
            onSuccess = {
                when (stripeIntent) {
                    is PaymentIntent -> {
                        it.handleNextActionForPaymentIntent(clientSecret)
                    }
                    is SetupIntent -> {
                        it.handleNextActionForSetupIntent(clientSecret)
                    }
                }
            },
            onFailure = ::error
        )
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
                            PaymentSelection.Saved.WalletType.GooglePay,
                        )
                        viewModel.paymentSelection = paymentSelection
                        confirmPaymentSelection(
                            paymentSelection,
                            state
                        )
                    },
                    onFailure = {
                        eventReporter.onPaymentFailure(
                            paymentSelection = PaymentSelection.GooglePay,
                            currency = viewModel.state?.stripeIntent?.currency,
                            isDecoupling = isDecoupling,
                        )
                        paymentResultCallback.onPaymentSheetResult(
                            PaymentSheetResult.Failed(it)
                        )
                    }
                )
            }
            is GooglePayPaymentMethodLauncher.Result.Failed -> {
                eventReporter.onPaymentFailure(
                    paymentSelection = PaymentSelection.GooglePay,
                    currency = viewModel.state?.stripeIntent?.currency,
                    isDecoupling = isDecoupling,
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

    fun onLinkActivityResult(result: LinkActivityResult): Unit = when (result) {
        is LinkActivityResult.Canceled -> onPaymentResult(PaymentResult.Canceled)
        is LinkActivityResult.Failed -> onPaymentResult(PaymentResult.Failed(result.error))
        is LinkActivityResult.Completed -> {
            runCatching {
                requireNotNull(viewModel.state)
            }.fold(
                onSuccess = { state ->
                    val paymentSelection = PaymentSelection.Saved(
                        result.paymentMethod,
                        PaymentSelection.Saved.WalletType.Link,
                    )
                    viewModel.paymentSelection = paymentSelection
                    confirmPaymentSelection(
                        paymentSelection,
                        state
                    )
                },
                onFailure = {
                    eventReporter.onPaymentFailure(
                        paymentSelection = PaymentSelection.Link,
                        currency = viewModel.state?.stripeIntent?.currency,
                        isDecoupling = isDecoupling,
                    )
                    paymentResultCallback.onPaymentSheetResult(
                        PaymentSheetResult.Failed(it)
                    )
                }
            )
        }
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
            is PaymentOptionResult.Failed -> {
                paymentOptionCallback.onPaymentOption(
                    viewModel.paymentSelection?.let {
                        paymentOptionFactory.create(it)
                    }
                )
            }
            is PaymentOptionResult.Canceled -> {
                val paymentSelection = paymentOptionResult.paymentSelection
                viewModel.paymentSelection = paymentSelection

                val paymentOption = paymentSelection?.let { paymentOptionFactory.create(it) }
                paymentOptionCallback.onPaymentOption(paymentOption)
            }
            null -> {
                viewModel.paymentSelection = null
                paymentOptionCallback.onPaymentOption(null)
            }
        }
    }

    internal fun onPaymentResult(paymentResult: PaymentResult) {
        logPaymentResult(paymentResult)
        viewModelScope.launch {
            paymentResultCallback.onPaymentSheetResult(
                paymentResult.convertToPaymentSheetResult()
            )
        }
    }

    private fun logPaymentResult(paymentResult: PaymentResult?) {
        when (paymentResult) {
            is PaymentResult.Completed -> {
                eventReporter.onPaymentSuccess(
                    paymentSelection = viewModel.paymentSelection,
                    currency = viewModel.state?.stripeIntent?.currency,
                    deferredIntentConfirmationType = viewModel.deferredIntentConfirmationType,
                )
                viewModel.deferredIntentConfirmationType = null
            }
            is PaymentResult.Failed -> {
                eventReporter.onPaymentFailure(
                    paymentSelection = viewModel.paymentSelection,
                    currency = viewModel.state?.stripeIntent?.currency,
                    isDecoupling = isDecoupling,
                )
            }
            else -> {
                // Nothing to do here
            }
        }
    }

    private fun confirmLink(
        paymentSelection: PaymentSelection,
        state: PaymentSheetState.Full
    ) {
        val linkConfig = requireNotNull(state.linkState).configuration

        viewModelScope.launch {
            val accountStatus = linkConfigurationCoordinator.getAccountStatusFlow(linkConfig).first()

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
            lifecycleScope = viewModelScope,
            config = googlePayPaymentLauncherConfig,
            readyCallback = {},
            activityResultLauncher = googlePayActivityLauncher,
            skipReadyCheck = true
        ).present(
            currencyCode = (state.stripeIntent as? PaymentIntent)?.currency
                ?: googlePayConfig.currencyCode.orEmpty(),
            amount = (state.stripeIntent as? PaymentIntent)?.amount ?: 0L,
            transactionId = state.stripeIntent.id
        )
    }

    private fun PaymentResult.convertToPaymentSheetResult() = when (this) {
        is PaymentResult.Completed -> PaymentSheetResult.Completed
        is PaymentResult.Canceled -> PaymentSheetResult.Canceled
        is PaymentResult.Failed -> PaymentSheetResult.Failed(throwable)
    }

    class GooglePayException(
        val throwable: Throwable
    ) : Exception(throwable)

    @Parcelize
    data class Args(
        val clientSecret: String,
        val config: PaymentSheet.Configuration?
    ) : Parcelable

    private fun <I, O> ActivityResultRegistryOwner.register(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> {
        val key = "FlowController_${contract::class.java.name}"
        return activityResultRegistry.register(key, contract, callback)
    }

    companion object {
        fun getInstance(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            statusBarColor: () -> Int?,
            paymentOptionCallback: PaymentOptionCallback,
            paymentResultCallback: PaymentSheetResultCallback
        ): PaymentSheet.FlowController {
            val injectorKey = WeakMapInjectorRegistry.nextKey(
                prefix = requireNotNull(PaymentSheet.FlowController::class.simpleName),
            )

            val flowControllerViewModel = ViewModelProvider(
                owner = viewModelStoreOwner,
            )[FlowControllerViewModel::class.java]

            val flowControllerStateComponent = flowControllerViewModel.flowControllerStateComponent

            val flowControllerComponent: FlowControllerComponent =
                flowControllerStateComponent.flowControllerComponentBuilder
                    .lifeCycleOwner(lifecycleOwner)
                    .activityResultRegistryOwner(activityResultRegistryOwner)
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
