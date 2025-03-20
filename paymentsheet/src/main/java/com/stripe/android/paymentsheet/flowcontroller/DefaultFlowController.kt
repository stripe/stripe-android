package com.stripe.android.paymentsheet.flowcontroller

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.InitializedViaCompose
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isLink
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.ui.SepaMandateContract
import com.stripe.android.paymentsheet.ui.SepaMandateResult
import com.stripe.android.paymentsheet.utils.canSave
import com.stripe.android.paymentsheet.utils.toConfirmationError
import com.stripe.android.uicore.utils.AnimationConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Named

@FlowControllerScope
internal class DefaultFlowController @Inject internal constructor(
    // Properties provided through FlowControllerComponent.Builder
    private val viewModelScope: CoroutineScope,
    private val lifecycleOwner: LifecycleOwner,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback,
    private val prefsRepositoryFactory: @JvmSuppressWildcards (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    activityResultCaller: ActivityResultCaller,
    // Properties provided through injection
    private val context: Context,
    private val eventReporter: EventReporter,
    private val viewModel: FlowControllerViewModel,
    private val confirmationHandler: ConfirmationHandler,
    private val linkHandler: LinkHandler,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
    private val configurationHandler: FlowControllerConfigurationHandler,
    private val errorReporter: ErrorReporter,
    @InitializedViaCompose private val initializedViaCompose: Boolean,
    @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
) : PaymentSheet.FlowController {
    private val paymentOptionActivityLauncher: ActivityResultLauncher<PaymentOptionContract.Args>
    private val sepaMandateActivityLauncher: ActivityResultLauncher<SepaMandateContract.Args>

    /**
     * [FlowControllerComponent] is hold to inject into [Activity]s and created
     * after [DefaultFlowController].
     */
    lateinit var flowControllerComponent: FlowControllerComponent

    private val initializationMode: PaymentElementLoader.InitializationMode?
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
        confirmationHandler.register(activityResultCaller, lifecycleOwner)

        paymentOptionActivityLauncher = activityResultCaller.registerForActivityResult(
            PaymentOptionContract(),
            ::onPaymentOptionResult
        )

        sepaMandateActivityLauncher = activityResultCaller.registerForActivityResult(
            SepaMandateContract(),
            ::onSepaMandateResult,
        )

        val activityResultLaunchers = setOf(
            paymentOptionActivityLauncher,
            sepaMandateActivityLauncher,
        )

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    activityResultLaunchers.forEach { it.unregister() }
                    PaymentElementCallbackReferences.remove(paymentElementCallbackIdentifier)
                }
            }
        )

        lifecycleOwner.lifecycleScope.launch {
            confirmationHandler.state.collectLatest { state ->
                when (state) {
                    is ConfirmationHandler.State.Idle,
                    is ConfirmationHandler.State.Confirming -> Unit
                    is ConfirmationHandler.State.Complete -> onIntentResult(state.result)
                }
            }
        }
    }

    override fun configureWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        configure(
            mode = PaymentElementLoader.InitializationMode.PaymentIntent(paymentIntentClientSecret),
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
            mode = PaymentElementLoader.InitializationMode.SetupIntent(setupIntentClientSecret),
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
            mode = PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration),
            configuration = configuration ?: PaymentSheet.Configuration.default(context),
            callback = callback,
        )
    }

    private fun configure(
        mode: PaymentElementLoader.InitializationMode,
        configuration: PaymentSheet.Configuration,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        configurationHandler.configure(
            scope = viewModelScope,
            initializationMode = mode,
            configuration = configuration,
            callback = callback,
            initializedViaCompose = initializedViaCompose,
        )
    }

    override fun getPaymentOption(): PaymentOption? {
        return viewModel.paymentSelection?.let {
            paymentOptionFactory.create(it)
        }
    }

    private fun currentStateForPresenting(): Result<State> {
        val state = viewModel.state
            ?: return Result.failure(
                IllegalStateException(
                    "FlowController must be successfully initialized " +
                        "using configureWithPaymentIntent(), configureWithSetupIntent() or " +
                        "configureWithIntentConfiguration() before calling presentPaymentOptions()."
                )
            )

        if (!configurationHandler.isConfigured) {
            return Result.failure(
                IllegalStateException(
                    "FlowController is not configured, or has a configuration update in flight."
                )
            )
        }

        return Result.success(state)
    }

    override fun presentPaymentOptions() {
        val stateResult = currentStateForPresenting()
        val state = stateResult.fold(
            onSuccess = {
                it
            },
            onFailure = {
                paymentResultCallback.onPaymentSheetResult(PaymentSheetResult.Failed(it))
                return
            }
        )

        val args = PaymentOptionContract.Args(
            state = state.paymentSheetState.copy(paymentSelection = viewModel.paymentSelection),
            configuration = state.config,
            enableLogging = enableLogging,
            productUsage = productUsage,
        )

        val options = ActivityOptionsCompat.makeCustomAnimation(
            viewModel.getApplication(),
            AnimationConstants.FADE_IN,
            AnimationConstants.FADE_OUT,
        )

        try {
            paymentOptionActivityLauncher.launch(args, options)
        } catch (e: IllegalStateException) {
            val message = "The host activity is not in a valid state (${lifecycleOwner.lifecycle.currentState})."
            paymentResultCallback.onPaymentSheetResult(PaymentSheetResult.Failed(IllegalStateException(message, e)))
        }
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

        val initializationMode = requireNotNull(initializationMode)

        when (val paymentSelection = viewModel.paymentSelection) {
            is PaymentSelection.Link,
            is PaymentSelection.New.LinkInline,
            is PaymentSelection.GooglePay,
            is PaymentSelection.ExternalPaymentMethod,
            is PaymentSelection.New,
            null -> confirmPaymentSelection(
                paymentSelection = paymentSelection,
                state = state.paymentSheetState,
                appearance = state.config.appearance,
                initializationMode = initializationMode,
            )
            is PaymentSelection.Saved -> confirmSavedPaymentMethod(
                paymentSelection = paymentSelection,
                state = state.paymentSheetState,
                appearance = state.config.appearance,
                initializationMode = initializationMode,
            )
        }
    }

    private fun confirmSavedPaymentMethod(
        paymentSelection: PaymentSelection.Saved,
        state: PaymentSheetState.Full,
        appearance: PaymentSheet.Appearance,
        initializationMode: PaymentElementLoader.InitializationMode,
    ) {
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
            confirmPaymentSelection(paymentSelection, state, appearance, initializationMode)
        }
    }

    @VisibleForTesting
    fun confirmPaymentSelection(
        paymentSelection: PaymentSelection?,
        state: PaymentSheetState.Full,
        appearance: PaymentSheet.Appearance,
        initializationMode: PaymentElementLoader.InitializationMode,
    ) {
        viewModelScope.launch {
            val confirmationOption = paymentSelection?.toConfirmationOption(
                configuration = state.config,
                linkConfiguration = state.paymentMethodMetadata.linkState?.configuration,
            )

            confirmationOption?.let { option ->
                val stripeIntent = requireNotNull(state.stripeIntent)

                confirmationHandler.start(
                    arguments = ConfirmationHandler.Args(
                        confirmationOption = option,
                        intent = stripeIntent,
                        initializationMode = initializationMode,
                        appearance = appearance,
                        shippingDetails = state.config.shippingDetails,
                    )
                )
            } ?: run {
                val message = paymentSelection?.let {
                    "Cannot confirm using a ${it::class.qualifiedName} payment selection!"
                } ?: "Cannot confirm without a payment selection!"

                val exception = IllegalStateException(message)

                paymentSelection?.let {
                    val event = ErrorReporter.UnexpectedErrorEvent.FLOW_CONTROLLER_INVALID_PAYMENT_SELECTION_ON_CHECKOUT
                    errorReporter.report(event, StripeException.create(exception))
                }

                onIntentResult(
                    ConfirmationHandler.Result.Failed(
                        cause = exception,
                        message = exception.stripeErrorMessage(),
                        type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
                    )
                )
            }
        }
    }

    @JvmSynthetic
    internal fun onPaymentOptionResult(
        paymentOptionResult: PaymentOptionResult?
    ) {
        paymentOptionResult?.paymentMethods?.let {
            val currentState = viewModel.state

            viewModel.state = currentState?.copyPaymentSheetState(
                customer = currentState.paymentSheetState.customer?.copy(paymentMethods = it)
            )
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

    private fun onIntentResult(result: ConfirmationHandler.Result) {
        when (result) {
            is ConfirmationHandler.Result.Succeeded -> {
                val stripeIntent = result.intent
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
                    is PaymentSelection.Saved -> {
                        when (currentSelection.walletType) {
                            PaymentSelection.Saved.WalletType.GooglePay -> PaymentSelection.GooglePay
                            PaymentSelection.Saved.WalletType.Link -> PaymentSelection.Link()
                            else -> currentSelection
                        }
                    }
                    else -> currentSelection
                }?.let {
                    prefsRepositoryFactory(viewModel.state?.config?.customer).savePaymentSelection(it)
                }

                eventReporter.onPaymentSuccess(
                    paymentSelection = viewModel.paymentSelection,
                    deferredIntentConfirmationType = result.deferredIntentConfirmationType,
                )

                onPaymentResult(
                    paymentResult = PaymentResult.Completed,
                    deferredIntentConfirmationType = result.deferredIntentConfirmationType,
                    shouldLog = false,
                )
            }
            is ConfirmationHandler.Result.Failed -> {
                val error = result.toConfirmationError()

                error?.let {
                    eventReporter.onPaymentFailure(
                        paymentSelection = viewModel.paymentSelection,
                        error = it
                    )
                }

                onPaymentResult(
                    paymentResult = PaymentResult.Failed(result.cause),
                    deferredIntentConfirmationType = null,
                    shouldLog = false,
                )
            }
            is ConfirmationHandler.Result.Canceled -> {
                handleCancellation(result)
            }
        }
    }

    private fun handleCancellation(canceled: ConfirmationHandler.Result.Canceled) {
        when (canceled.action) {
            ConfirmationHandler.Result.Canceled.Action.InformCancellation -> {
                onPaymentResult(
                    paymentResult = PaymentResult.Canceled,
                    deferredIntentConfirmationType = null,
                    shouldLog = false,
                )
            }
            ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails -> presentPaymentOptions()
            ConfirmationHandler.Result.Canceled.Action.None -> Unit
        }
    }

    internal fun onPaymentResult(
        paymentResult: PaymentResult,
        deferredIntentConfirmationType: DeferredIntentConfirmationType? = null,
        shouldLog: Boolean = true,
    ) {
        if (shouldLog) {
            logPaymentResult(paymentResult, deferredIntentConfirmationType)
        }

        val selection = viewModel.paymentSelection

        if (paymentResult is PaymentResult.Completed && selection != null && selection.isLink) {
            linkHandler.logOut()
        }

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

    private fun logPaymentResult(
        paymentResult: PaymentResult?,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?
    ) {
        when (paymentResult) {
            is PaymentResult.Completed -> {
                eventReporter.onPaymentSuccess(
                    paymentSelection = viewModel.paymentSelection,
                    deferredIntentConfirmationType = deferredIntentConfirmationType,
                )
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

    private fun PaymentResult.convertToPaymentSheetResult() = when (this) {
        is PaymentResult.Completed -> PaymentSheetResult.Completed
        is PaymentResult.Canceled -> PaymentSheetResult.Canceled
        is PaymentResult.Failed -> PaymentSheetResult.Failed(throwable)
    }

    @Parcelize
    data class Args(
        val clientSecret: String,
        val config: PaymentSheet.Configuration?
    ) : Parcelable

    @Parcelize
    data class State(
        val paymentSheetState: PaymentSheetState.Full,
        val config: PaymentSheet.Configuration,
    ) : Parcelable {
        fun copyPaymentSheetState(
            paymentSelection: PaymentSelection? = paymentSheetState.paymentSelection,
            customer: CustomerState? = paymentSheetState.customer,
        ): State = copy(
            paymentSheetState = paymentSheetState.copy(
                paymentSelection = paymentSelection,
                customer = customer,
            )
        )
    }

    companion object {
        fun getInstance(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            activityResultCaller: ActivityResultCaller,
            statusBarColor: () -> Int?,
            paymentOptionCallback: PaymentOptionCallback,
            paymentResultCallback: PaymentSheetResultCallback,
            paymentElementCallbackIdentifier: String,
            initializedViaCompose: Boolean,
        ): PaymentSheet.FlowController {
            val flowControllerViewModel = ViewModelProvider(
                owner = viewModelStoreOwner,
                factory = FlowControllerViewModel.Factory(statusBarColor(), paymentElementCallbackIdentifier),
            ).get(
                key = "FlowControllerViewModel(instance = $paymentElementCallbackIdentifier)",
                modelClass = FlowControllerViewModel::class.java
            )

            val flowControllerStateComponent = flowControllerViewModel.flowControllerStateComponent

            val flowControllerComponent: FlowControllerComponent =
                flowControllerStateComponent.flowControllerComponentBuilder
                    .lifeCycleOwner(lifecycleOwner)
                    .activityResultCaller(activityResultCaller)
                    .paymentOptionCallback(paymentOptionCallback)
                    .paymentResultCallback(paymentResultCallback)
                    .initializedViaCompose(initializedViaCompose)
                    .build()
            val flowController = flowControllerComponent.flowController
            flowController.flowControllerComponent = flowControllerComponent
            return flowController
        }
    }
}
