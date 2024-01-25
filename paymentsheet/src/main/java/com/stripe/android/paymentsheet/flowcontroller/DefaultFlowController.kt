package com.stripe.android.paymentsheet.flowcontroller

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.intercept
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.ui.SepaMandateContract
import com.stripe.android.paymentsheet.ui.SepaMandateResult
import com.stripe.android.paymentsheet.utils.canSave
import com.stripe.android.utils.AnimationConstants
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
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
    private val prefsRepositoryFactory: @JvmSuppressWildcards (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    // Properties provided through injection
    private val context: Context,
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
    bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
    private val linkLauncher: LinkPaymentLauncher,
    private val configurationHandler: FlowControllerConfigurationHandler,
    private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
) : PaymentSheet.FlowController {
    private val paymentOptionActivityLauncher: ActivityResultLauncher<PaymentOptionContract.Args>
    private val googlePayActivityLauncher:
        ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>
    private val sepaMandateActivityLauncher: ActivityResultLauncher<SepaMandateContract.Args>
    private val bacsMandateConfirmationLauncher: BacsMandateConfirmationLauncher

    /**
     * [FlowControllerComponent] is hold to inject into [Activity]s and created
     * after [DefaultFlowController].
     */
    lateinit var flowControllerComponent: FlowControllerComponent

    private var paymentLauncher: StripePaymentLauncher? = null

    private val initializationMode: PaymentSheet.InitializationMode?
        get() = viewModel.previousConfigureRequest?.initializationMode

    override var shippingDetails: AddressDetails?
        get() = viewModel.state?.config?.shippingDetails
        set(value) {
            val state = viewModel.state
            if (state != null) {
                viewModel.state = state.copy(
                    config = state.config.copy(
                        shippingDetails = value
                    )
                )
            }
        }

    init {
        val paymentLauncherActivityResultLauncher = activityResultRegistryOwner.register(
            PaymentLauncherContract(),
            ::onInternalPaymentResult
        )

        paymentOptionActivityLauncher = activityResultRegistryOwner.register(
            PaymentOptionContract(),
            ::onPaymentOptionResult
        )

        googlePayActivityLauncher = activityResultRegistryOwner.register(
            GooglePayPaymentMethodLauncherContractV2(),
            ::onGooglePayResult
        )

        sepaMandateActivityLauncher = activityResultRegistryOwner.register(
            SepaMandateContract(),
            ::onSepaMandateResult,
        )

        val bacsMandateConfirmationActivityLauncher = activityResultRegistryOwner.register(
            BacsMandateConfirmationContract(),
            ::onBacsMandateResult
        )

        bacsMandateConfirmationLauncher = bacsMandateConfirmationLauncherFactory.create(
            bacsMandateConfirmationActivityLauncher
        )

        val activityResultLaunchers = setOf(
            paymentLauncherActivityResultLauncher,
            paymentOptionActivityLauncher,
            googlePayActivityLauncher,
            sepaMandateActivityLauncher,
            bacsMandateConfirmationActivityLauncher
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
                        includePaymentSheetAuthenticators = true,
                    )
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    activityResultLaunchers.forEach { it.unregister() }
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
            configuration = configuration ?: PaymentSheet.Configuration.default(context),
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
            configuration = configuration ?: PaymentSheet.Configuration.default(context),
            callback = callback,
        )
    }

    override fun configureWithIntentConfiguration(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        configure(
            mode = PaymentSheet.InitializationMode.DeferredIntent(intentConfiguration),
            configuration = configuration ?: PaymentSheet.Configuration.default(context),
            callback = callback,
        )
    }

    private fun configure(
        mode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration,
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
            is PaymentSelection.New.GenericPaymentMethod -> {
                if (paymentSelection.paymentMethodCreateParams.typeCode == PaymentMethod.Type.BacsDebit.code) {
                    BacsMandateData.fromPaymentSelection(paymentSelection)?.let { data ->
                        bacsMandateConfirmationLauncher.launch(
                            data = data,
                            appearance = getPaymentAppearance()
                        )
                    } ?: run {
                        paymentResultCallback.onPaymentSheetResult(
                            PaymentSheetResult.Failed(
                                BacsMandateException(
                                    type = BacsMandateException.Type.MissingInformation
                                )
                            )
                        )
                    }
                } else {
                    confirmPaymentSelection(paymentSelection, state)
                }
            }
            is PaymentSelection.New,
            null -> confirmPaymentSelection(paymentSelection, state)
            is PaymentSelection.Saved -> {
                if (paymentSelection.paymentMethod.type == PaymentMethod.Type.SepaDebit &&
                    viewModel.paymentSelection?.hasAcknowledgedSepaMandate == false
                ) {
                    // We're legally required to show the customer the SEPA mandate before every payment/setup.
                    // In the edge case where the customer never opened the sheet, and thus never saw the mandate,
                    // we present the mandate directly.
                    sepaMandateActivityLauncher.launch(
                        SepaMandateContract.Args(
                            merchantName = state.config.merchantDisplayName
                        )
                    )
                } else {
                    confirmPaymentSelection(paymentSelection, state)
                }
            }
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
                shippingValues = state.config.shippingDetails?.toConfirmPaymentIntentShipping(),
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
                    onFailure = { error ->
                        eventReporter.onPaymentFailure(
                            paymentSelection = PaymentSelection.GooglePay,
                            error = PaymentSheetConfirmationError.InvalidState,
                        )
                        paymentResultCallback.onPaymentSheetResult(
                            PaymentSheetResult.Failed(error)
                        )
                    }
                )
            }
            is GooglePayPaymentMethodLauncher.Result.Failed -> {
                eventReporter.onPaymentFailure(
                    paymentSelection = PaymentSelection.GooglePay,
                    error = PaymentSheetConfirmationError.GooglePay(googlePayResult.errorCode),
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

    internal fun onBacsMandateResult(
        result: BacsMandateConfirmationResult
    ) {
        when (result) {
            is BacsMandateConfirmationResult.Confirmed -> {
                runCatching {
                    requireNotNull(viewModel.state)
                }.fold(
                    onSuccess = { state ->
                        val currentSelection = viewModel.paymentSelection

                        if (
                            currentSelection is PaymentSelection.New.GenericPaymentMethod &&
                            currentSelection.paymentMethodCreateParams.typeCode == PaymentMethod.Type.BacsDebit.code
                        ) {
                            confirmPaymentSelection(currentSelection, state)
                        } else {
                            paymentResultCallback.onPaymentSheetResult(
                                PaymentSheetResult.Failed(
                                    BacsMandateException(
                                        type = BacsMandateException.Type.IncorrectSelection
                                    )
                                )
                            )
                        }
                    },
                    onFailure = { error ->
                        paymentResultCallback.onPaymentSheetResult(
                            PaymentSheetResult.Failed(error)
                        )
                    }
                )
            }
            is BacsMandateConfirmationResult.ModifyDetails -> presentPaymentOptions()
            is BacsMandateConfirmationResult.Cancelled -> Unit
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
                onFailure = { error ->
                    eventReporter.onPaymentFailure(
                        paymentSelection = PaymentSelection.Link,
                        error = PaymentSheetConfirmationError.InvalidState,
                    )
                    paymentResultCallback.onPaymentSheetResult(
                        PaymentSheetResult.Failed(error)
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
                paymentSelection.hasAcknowledgedSepaMandate = true
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

    internal fun onInternalPaymentResult(internalPaymentResult: InternalPaymentResult) {
        val paymentResult = when (internalPaymentResult) {
            is InternalPaymentResult.Completed -> {
                val stripeIntent = internalPaymentResult.intent
                val currentSelection = viewModel.paymentSelection
                val currentInitializationMode = initializationMode

                /*
                 * Sets current selection as default payment method in future payment sheet usage. New payment
                 * methods are only saved if the payment sheet is in setup mode, is in payment intent with setup
                 * for usage, or the customer has requested the payment method be saved.
                 */
                when (currentSelection) {
                    is PaymentSelection.New -> stripeIntent.paymentMethod.takeIf {
                        currentInitializationMode != null && currentSelection.canSave(
                            initializationMode = currentInitializationMode
                        )
                    }?.let { method ->
                        PaymentSelection.Saved(method)
                    }
                    else -> currentSelection
                }?.let {
                    prefsRepositoryFactory(viewModel.state?.config?.customer).savePaymentSelection(it)
                }

                PaymentResult.Completed
            }
            is InternalPaymentResult.Failed -> PaymentResult.Failed(internalPaymentResult.throwable)
            is InternalPaymentResult.Canceled -> PaymentResult.Canceled
        }

        onPaymentResult(paymentResult)
    }

    internal fun onPaymentResult(paymentResult: PaymentResult) {
        logPaymentResult(paymentResult)
        viewModelScope.launch {
            paymentResultCallback.onPaymentSheetResult(
                paymentResult.convertToPaymentSheetResult()
            )
        }
    }

    internal fun onSepaMandateResult(sepaMandateResult: SepaMandateResult) {
        when (sepaMandateResult) {
            SepaMandateResult.Acknowledged -> {
                viewModel.paymentSelection?.hasAcknowledgedSepaMandate = true
                confirm()
            }
            SepaMandateResult.Canceled -> {
                paymentResultCallback.onPaymentSheetResult(PaymentSheetResult.Canceled)
            }
        }
    }

    private fun logPaymentResult(paymentResult: PaymentResult?) {
        when (paymentResult) {
            is PaymentResult.Completed -> {
                eventReporter.onPaymentSuccess(
                    paymentSelection = viewModel.paymentSelection,
                    deferredIntentConfirmationType = viewModel.deferredIntentConfirmationType,
                )
                viewModel.deferredIntentConfirmationType = null
            }
            is PaymentResult.Failed -> {
                eventReporter.onPaymentFailure(
                    paymentSelection = viewModel.paymentSelection,
                    error = PaymentSheetConfirmationError.Stripe(paymentResult.throwable),
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

        if (paymentSelection is PaymentSelection.Link) {
            // User selected Link as the payment method, not inline
            linkLauncher.present(linkConfig)
        } else {
            // New user paying inline, complete without launching Link
            confirmPaymentSelection(paymentSelection, state)
        }
    }

    private fun launchGooglePay(state: PaymentSheetState.Full) {
        // state.config.googlePay is guaranteed not to be null or GooglePay would be disabled
        val googlePayConfig = requireNotNull(state.config.googlePay)

        val googlePayPaymentLauncherConfig = GooglePayPaymentMethodLauncher.Config(
            environment = when (googlePayConfig.environment) {
                PaymentSheet.GooglePayConfiguration.Environment.Production ->
                    GooglePayEnvironment.Production
                else ->
                    GooglePayEnvironment.Test
            },
            merchantCountryCode = googlePayConfig.countryCode,
            merchantName = state.config.merchantDisplayName,
            isEmailRequired = state.config.billingDetailsCollectionConfiguration.collectsEmail,
            billingAddressConfig = state.config.billingDetailsCollectionConfiguration.toBillingAddressConfig(),
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
            transactionId = state.stripeIntent.id,
            label = googlePayConfig.label,
        )
    }

    private fun PaymentResult.convertToPaymentSheetResult() = when (this) {
        is PaymentResult.Completed -> PaymentSheetResult.Completed
        is PaymentResult.Canceled -> PaymentSheetResult.Canceled
        is PaymentResult.Failed -> PaymentSheetResult.Failed(throwable)
    }

    private fun getPaymentAppearance(): PaymentSheet.Appearance {
        return viewModel.state?.config?.appearance ?: PaymentSheet.Appearance()
    }

    class BacsMandateException(
        val type: Type
    ) : Exception() {
        override val message: String = when (type) {
            Type.MissingInformation ->
                "Bacs requires the account's name, email, sort code, and account number be provided!"
            Type.IncorrectSelection -> "Cannot confirm non-Bacs payment method with Bacs mandate"
        }

        enum class Type {
            MissingInformation,
            IncorrectSelection
        }
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
            val flowControllerViewModel = ViewModelProvider(
                owner = viewModelStoreOwner,
                factory = SavedStateViewModelFactory()
            )[FlowControllerViewModel::class.java]

            val flowControllerStateComponent = flowControllerViewModel.flowControllerStateComponent

            val flowControllerComponent: FlowControllerComponent =
                flowControllerStateComponent.flowControllerComponentBuilder
                    .lifeCycleOwner(lifecycleOwner)
                    .activityResultRegistryOwner(activityResultRegistryOwner)
                    .statusBarColor(statusBarColor)
                    .paymentOptionCallback(paymentOptionCallback)
                    .paymentResultCallback(paymentResultCallback)
                    .build()
            val flowController = flowControllerComponent.flowController
            flowController.flowControllerComponent = flowControllerComponent
            return flowController
        }
    }
}
