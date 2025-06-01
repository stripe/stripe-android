package com.stripe.android.paymentsheet.flowcontroller

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkActivityResult.Canceled.Reason
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.updateLinkAccount
import com.stripe.android.link.domain.LinkProminenceFeatureProvider
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.toLoginState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.WalletsButtonPreview
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
import com.stripe.android.paymentsheet.model.PaymentSelection.Link
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

@OptIn(WalletsButtonPreview::class)
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
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    // Properties provided through injection
    private val context: Context,
    private val eventReporter: EventReporter,
    private val viewModel: FlowControllerViewModel,
    private val confirmationHandler: ConfirmationHandler,
    private val linkProminenceFeatureProvider: LinkProminenceFeatureProvider,
    private val linkHandler: LinkHandler,
    private val linkAccountHolder: LinkAccountHolder,
    @Named(LINK_LAUNCHER_KEY) private val linkPaymentLauncher: LinkPaymentLauncher,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
    private val configurationHandler: FlowControllerConfigurationHandler,
    private val errorReporter: ErrorReporter,
    @InitializedViaCompose private val initializedViaCompose: Boolean,
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
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

        linkPaymentLauncher.register(
            key = LINK_LAUNCHER_KEY,
            activityResultRegistry = activityResultRegistryOwner.activityResultRegistry,
            callback = ::onLinkPaymentMethodSelected
        )

        val activityResultLaunchers = setOf(
            paymentOptionActivityLauncher,
            sepaMandateActivityLauncher,
        )

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    activityResultLaunchers.forEach { it.unregister() }
                    linkPaymentLauncher.unregister()
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

    @Composable
    override fun WalletButtons() {
        viewModel.flowControllerStateComponent.walletButtonsContent.Content()
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

    private fun withCurrentState(block: (State) -> Unit) {
        val state = viewModel.state
        if (state == null) {
            paymentResultCallback.onPaymentSheetResult(
                PaymentSheetResult.Failed(
                    IllegalStateException(
                        "FlowController must be successfully initialized " +
                            "using configureWithPaymentIntent(), configureWithSetupIntent() or " +
                            "configureWithIntentConfiguration() before calling presentPaymentOptions()."
                    )
                )
            )
        } else if (!configurationHandler.isConfigured) {
            paymentResultCallback.onPaymentSheetResult(
                PaymentSheetResult.Failed(
                    IllegalStateException(
                        "FlowController is not configured, or has a configuration update in flight."
                    )
                )
            )
        } else {
            block(state)
        }
    }

    override fun presentPaymentOptions() {
        withCurrentState { state ->
            val linkConfiguration = state.paymentSheetState.linkConfiguration
            val paymentSelection = viewModel.paymentSelection
            val linkAccountInfo = linkAccountHolder.linkAccountInfo.value

            val shouldPresentLinkInsteadOfPaymentOptions =
                // The current payment selection is Link
                paymentSelection?.isLink == true &&
                    // The current user has a Link account (not necessarily logged in)
                    linkAccountInfo.account != null &&
                    // Link is enabled and available
                    linkConfiguration != null &&
                    // feature flag and other conditions are met
                    linkProminenceFeatureProvider.shouldShowEarlyVerificationInFlowController(linkConfiguration)

            if (shouldPresentLinkInsteadOfPaymentOptions) {
                linkPaymentLauncher.present(
                    configuration = linkConfiguration,
                    linkAccountInfo = linkAccountInfo,
                    useLinkExpress = true,
                    launchMode = LinkLaunchMode.PaymentMethodSelection(
                        selectedPayment = (paymentSelection as? Link)?.selectedPayment?.details
                    )
                )
            } else {
                showPaymentOptionList(state, paymentSelection)
            }
        }
    }

    private fun showPaymentOptionList(
        state: State,
        paymentSelection: PaymentSelection?
    ) {
        val args = PaymentOptionContract.Args(
            state = state.paymentSheetState.copy(paymentSelection = paymentSelection),
            configuration = state.config,
            enableLogging = enableLogging,
            productUsage = productUsage,
            linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier
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

    fun onLinkPaymentMethodSelected(result: LinkActivityResult) {
        result.linkAccountUpdate?.updateLinkAccount()
        when (result) {
            is LinkActivityResult.PaymentMethodObtained,
            is LinkActivityResult.Failed -> Unit
            is LinkActivityResult.Canceled -> when (result.reason) {
                Reason.BackPressed -> withCurrentState {
                    // just show the payment option list if
                    // the user didn't have any preselected Link payment details
                    if (viewModel.paymentSelection?.readyToPayWithLink() == false) {
                        showPaymentOptionList(it, viewModel.paymentSelection)
                    }
                }
                Reason.LoggedOut -> {
                    updateLinkPaymentSelection(null)
                    withCurrentState { showPaymentOptionList(it, viewModel.paymentSelection) }
                }
                Reason.PayAnotherWay -> {
                    withCurrentState { showPaymentOptionList(it, viewModel.paymentSelection) }
                }
            }

            is LinkActivityResult.Completed -> {
                updateLinkPaymentSelection(result.selectedPayment)
            }
        }
    }

    fun PaymentSelection.readyToPayWithLink(): Boolean = when (this) {
        is Link -> selectedPayment != null
        else -> isLink
    }

    /**
     * Updates the Link account state in FlowController after receiving a [LinkAccountUpdate]
     *
     * - Calls [LinkAccountHolder.set] to update the Link account state
     * - Updates the Link account state in the [PaymentSheetState] with the latest [AccountStatus]
     */
    private fun LinkAccountUpdate.updateLinkAccount() {
        updateLinkAccount(linkAccountHolder)
        when (this) {
            is LinkAccountUpdate.Value -> {
                val currentState = viewModel.state ?: return
                val metadata = currentState.paymentSheetState.paymentMethodMetadata
                val accountStatus = account?.accountStatus ?: AccountStatus.SignedOut
                val linkState = metadata.linkState?.copy(loginState = accountStatus.toLoginState())
                viewModel.state = currentState.copyPaymentSheetState(
                    metadata = metadata.copy(linkState = linkState),
                )
            }
            LinkAccountUpdate.None -> Unit
        }
    }

    /**
     * If the current payment selection is Link and Link details changed, update the payment selection accordingly.
     */
    private fun updateLinkPaymentSelection(
        linkPaymentMethod: LinkPaymentMethod?
    ) {
        val paymentSelection = viewModel.paymentSelection
        if (paymentSelection is Link) {
            val updated = paymentSelection.copy(
                selectedPayment = linkPaymentMethod
            )
            viewModel.paymentSelection = updated
            paymentOptionCallback.onPaymentOption(paymentOptionFactory.create(updated))
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
            is Link,
            is PaymentSelection.New.LinkInline,
            is PaymentSelection.GooglePay,
            is PaymentSelection.ExternalPaymentMethod,
            is PaymentSelection.CustomPaymentMethod,
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
                linkConfiguration = state.linkConfiguration,
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
        result: PaymentOptionResult?
    ) {
        // update the current Link account state if the selected Link payment method includes an account update.
        result?.linkAccountInfo?.let { linkAccountHolder.set(it) }
        result?.paymentMethods?.let {
            val currentState = viewModel.state
            viewModel.state = currentState?.copyPaymentSheetState(
                customer = currentState.paymentSheetState.customer?.copy(paymentMethods = it)
            )
        }
        when (result) {
            is PaymentOptionResult.Succeeded -> {
                viewModel.paymentSelection = result.paymentSelection.also { it.hasAcknowledgedSepaMandate = true }
                onPaymentSelection()
            }
            null,
            is PaymentOptionResult.Canceled -> {
                viewModel.paymentSelection = result?.paymentSelection
                onPaymentSelection()
            }
            is PaymentOptionResult.Failed -> {
                onPaymentSelection()
            }
        }
    }

    private fun onPaymentSelection() {
        val paymentSelection = viewModel.paymentSelection
        val paymentOption = paymentSelection?.let { paymentOptionFactory.create(it) }
        paymentOptionCallback.onPaymentOption(paymentOption)
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
                            PaymentSelection.Saved.WalletType.Link -> Link()
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

        if (paymentResult is PaymentResult.Completed) {
            viewModel.paymentSelection = null
            viewModel.state = null
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
            metadata: PaymentMethodMetadata = paymentSheetState.paymentMethodMetadata
        ): State = copy(
            paymentSheetState = paymentSheetState.copy(
                paymentSelection = paymentSelection,
                customer = customer,
                paymentMethodMetadata = metadata
            )
        )

        val linkConfiguration
            get() = paymentSheetState.paymentMethodMetadata.linkState?.configuration
    }

    companion object {
        internal const val LINK_LAUNCHER_KEY = "LinkPaymentLauncher_DefaultFlowController"

        fun getInstance(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            activityResultCaller: ActivityResultCaller,
            statusBarColor: () -> Int?,
            paymentOptionCallback: PaymentOptionCallback,
            paymentResultCallback: PaymentSheetResultCallback,
            paymentElementCallbackIdentifier: String,
            initializedViaCompose: Boolean,
            activityResultRegistryOwner: ActivityResultRegistryOwner,
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
                    .activityResultRegistryOwner(activityResultRegistryOwner)
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
