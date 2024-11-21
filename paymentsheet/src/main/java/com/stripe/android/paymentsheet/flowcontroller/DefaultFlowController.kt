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
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.DefaultConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import com.stripe.android.paymentsheet.InitializedViaCompose
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
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isLink
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionContract
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionResult
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.ui.SepaMandateContract
import com.stripe.android.paymentsheet.ui.SepaMandateResult
import com.stripe.android.paymentsheet.utils.canSave
import com.stripe.android.uicore.utils.AnimationConstants
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@FlowControllerScope
internal class DefaultFlowController @Inject internal constructor(
    // Properties provided through FlowControllerComponent.Builder
    private val viewModelScope: CoroutineScope,
    private val lifecycleOwner: LifecycleOwner,
    private val statusBarColor: () -> Int?,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val paymentResultCallback: PaymentSheetResultCallback,
    private val prefsRepositoryFactory: @JvmSuppressWildcards (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    activityResultCaller: ActivityResultCaller,
    // Properties provided through injection
    private val context: Context,
    private val eventReporter: EventReporter,
    private val viewModel: FlowControllerViewModel,
    paymentLauncherFactory: StripePaymentLauncherAssistedFactory,
    /**
     * [PaymentConfiguration] is [Lazy] because the client might set publishableKey and
     * stripeAccountId after creating a [DefaultFlowController].
     */
    lazyPaymentConfiguration: Provider<PaymentConfiguration>,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
    googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
    bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
    cvcRecollectionLauncherFactory: CvcRecollectionLauncherFactory,
    private val linkLauncher: LinkPaymentLauncher,
    private val configurationHandler: FlowControllerConfigurationHandler,
    intentConfirmationInterceptor: IntentConfirmationInterceptor,
    private val errorReporter: ErrorReporter,
    @InitializedViaCompose private val initializedViaCompose: Boolean,
    @IOContext workContext: CoroutineContext,
    logger: UserFacingLogger,
    private val cvcRecollectionHandler: CvcRecollectionHandler
) : PaymentSheet.FlowController {
    private val paymentOptionActivityLauncher: ActivityResultLauncher<PaymentOptionContract.Args>
    private val sepaMandateActivityLauncher: ActivityResultLauncher<SepaMandateContract.Args>
    private val cvcRecollectionLauncher: CvcRecollectionLauncher

    /**
     * [FlowControllerComponent] is hold to inject into [Activity]s and created
     * after [DefaultFlowController].
     */
    lateinit var flowControllerComponent: FlowControllerComponent

    private val confirmationHandler = DefaultConfirmationHandler.Factory(
        intentConfirmationInterceptor = intentConfirmationInterceptor,
        paymentConfigurationProvider = lazyPaymentConfiguration,
        statusBarColor = { null },
        savedStateHandle = viewModel.handle,
        bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
        stripePaymentLauncherAssistedFactory = paymentLauncherFactory,
        googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory,
        errorReporter = errorReporter,
        logger = logger,
    ).create(viewModelScope.plus(workContext))

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

        val cvcRecollectionActivityLauncher = activityResultCaller.registerForActivityResult(
            CvcRecollectionContract(),
            ::onCvcRecollectionResult
        )

        cvcRecollectionLauncher = cvcRecollectionLauncherFactory.create(
            cvcRecollectionActivityLauncher
        )

        val activityResultLaunchers = setOf(
            paymentOptionActivityLauncher,
            sepaMandateActivityLauncher,
            cvcRecollectionActivityLauncher
        )

        linkLauncher.register(
            activityResultCaller = activityResultCaller,
            callback = ::onLinkActivityResult,
        )

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    activityResultLaunchers.forEach { it.unregister() }
                    linkLauncher.unregister()
                    PaymentSheet.FlowController.linkHandler = null
                    IntentConfirmationInterceptor.createIntentCallback = null
                    ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = null
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
            statusBarColor = statusBarColor(),
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

        when (val paymentSelection = viewModel.paymentSelection) {
            is PaymentSelection.Link,
            is PaymentSelection.New.LinkInline -> confirmLink(
                paymentSelection = paymentSelection,
                state = state.paymentSheetState,
                appearance = state.config.appearance,
            )
            is PaymentSelection.GooglePay,
            is PaymentSelection.ExternalPaymentMethod,
            is PaymentSelection.New,
            null -> confirmPaymentSelection(paymentSelection, state.paymentSheetState, state.config.appearance)
            is PaymentSelection.Saved -> confirmSavedPaymentMethod(
                paymentSelection = paymentSelection,
                state = state.paymentSheetState,
                appearance = state.config.appearance,
            )
        }
    }

    private fun confirmSavedPaymentMethod(
        paymentSelection: PaymentSelection.Saved,
        state: PaymentSheetState.Full,
        appearance: PaymentSheet.Appearance,
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
        } else if (
            cvcRecollectionHandler.requiresCVCRecollection(
                stripeIntent = state.stripeIntent,
                paymentSelection = paymentSelection,
                initializationMode = initializationMode
            )
        ) {
            cvcRecollectionHandler.launch(paymentSelection) { cvcRecollectionData ->
                cvcRecollectionLauncher.launch(
                    data = cvcRecollectionData,
                    appearance = getPaymentAppearance(),
                    isLiveMode = state.stripeIntent.isLiveMode
                )
            }
        } else {
            confirmPaymentSelection(paymentSelection, state, appearance)
        }
    }

    @VisibleForTesting
    fun confirmPaymentSelection(
        paymentSelection: PaymentSelection?,
        state: PaymentSheetState.Full,
        appearance: PaymentSheet.Appearance,
    ) {
        viewModelScope.launch {
            val initializationMode = requireNotNull(initializationMode)

            val confirmationOption = paymentSelection?.toConfirmationOption(
                initializationMode = initializationMode,
                configuration = state.config,
                appearance = appearance,
            )

            confirmationOption?.let { option ->
                val stripeIntent = requireNotNull(state.stripeIntent)

                confirmationHandler.start(
                    arguments = ConfirmationHandler.Args(
                        confirmationOption = option,
                        intent = stripeIntent,
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

    internal fun onCvcRecollectionResult(
        result: CvcRecollectionResult
    ) {
        when (result) {
            is CvcRecollectionResult.Cancelled -> Unit
            is CvcRecollectionResult.Confirmed -> {
                runCatching {
                    requireNotNull(viewModel.state)
                }.fold(
                    onSuccess = { state ->
                        (viewModel.paymentSelection as? PaymentSelection.Saved)?.let {
                            val selection = PaymentSelection.Saved(
                                paymentMethod = it.paymentMethod,
                                walletType = it.walletType,
                                paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                                    cvc = result.cvc,
                                )
                            )
                            viewModel.paymentSelection = selection
                            confirmPaymentSelection(selection, state.paymentSheetState, state.config.appearance)
                        } ?: paymentResultCallback.onPaymentSheetResult(
                            PaymentSheetResult.Failed(
                                CvcRecollectionException(
                                    type = CvcRecollectionException.Type.IncorrectSelection
                                )
                            )
                        )
                        errorReporter.report(
                            ErrorReporter.UnexpectedErrorEvent.CVC_RECOLLECTION_UNEXPECTED_PAYMENT_SELECTION
                        )
                    },
                    onFailure = { error ->
                        paymentResultCallback.onPaymentSheetResult(
                            PaymentSheetResult.Failed(error)
                        )
                    }
                )
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
                        state.paymentSheetState,
                        state.config.appearance,
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
                            PaymentSelection.Saved.WalletType.Link -> PaymentSelection.Link
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
                val error = result.type.toConfirmationError(result.cause)

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

    @OptIn(DelicateCoroutinesApi::class)
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
            GlobalScope.launch {
                // This usage is intentional. We want the request to be sent without regard for the UI lifecycle.
                PaymentSheet.FlowController.linkHandler?.logOut()
            }
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

    private fun confirmLink(
        paymentSelection: PaymentSelection,
        state: PaymentSheetState.Full,
        appearance: PaymentSheet.Appearance,
    ) {
        val linkConfig = requireNotNull(state.linkState).configuration

        if (paymentSelection is PaymentSelection.Link) {
            // User selected Link as the payment method, not inline
            linkLauncher.present(linkConfig)
        } else {
            // New user paying inline, complete without launching Link
            confirmPaymentSelection(paymentSelection, state, appearance)
        }
    }

    private fun ConfirmationHandler.Result.Failed.ErrorType.toConfirmationError(
        cause: Throwable
    ): PaymentSheetConfirmationError? {
        return when (this) {
            ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod ->
                PaymentSheetConfirmationError.ExternalPaymentMethod
            ConfirmationHandler.Result.Failed.ErrorType.Payment ->
                PaymentSheetConfirmationError.Stripe(cause)
            is ConfirmationHandler.Result.Failed.ErrorType.GooglePay ->
                PaymentSheetConfirmationError.GooglePay(errorCode)
            ConfirmationHandler.Result.Failed.ErrorType.Internal,
            ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration,
            ConfirmationHandler.Result.Failed.ErrorType.Fatal -> null
        }
    }

    private fun PaymentResult.convertToPaymentSheetResult() = when (this) {
        is PaymentResult.Completed -> PaymentSheetResult.Completed
        is PaymentResult.Canceled -> PaymentSheetResult.Canceled
        is PaymentResult.Failed -> PaymentSheetResult.Failed(throwable)
    }

    private fun getPaymentAppearance(): PaymentSheet.Appearance {
        return viewModel.state?.config?.appearance ?: PaymentSheet.Appearance()
    }

    class CvcRecollectionException(
        val type: Type
    ) : Exception() {
        override val message: String = when (type) {
            Type.IncorrectSelection -> "PaymentSelection must be PaymentSelection.Saved for CVC recollection"
        }

        enum class Type {
            IncorrectSelection
        }
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
            initializedViaCompose: Boolean,
        ): PaymentSheet.FlowController {
            val flowControllerViewModel = ViewModelProvider(
                owner = viewModelStoreOwner,
                factory = SavedStateViewModelFactory()
            )[FlowControllerViewModel::class.java]

            val flowControllerStateComponent = flowControllerViewModel.flowControllerStateComponent

            val flowControllerComponent: FlowControllerComponent =
                flowControllerStateComponent.flowControllerComponentBuilder
                    .lifeCycleOwner(lifecycleOwner)
                    .activityResultCaller(activityResultCaller)
                    .statusBarColor(statusBarColor)
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
