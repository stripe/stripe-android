package com.stripe.android.paymentsheet.flowcontroller

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutConfigurationMerger
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkActivityResult.Canceled.Reason
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.updateLinkAccount
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.toLoginState
import com.stripe.android.link.utils.determineFallbackPaymentSelectionAfterLinkLogout
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentelement.WalletButtonsViewClickHandler
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationTypeKey
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.InitializedViaCompose
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.paymentsheet.PaymentOptionResultCallback
import com.stripe.android.paymentsheet.PaymentOptionsActivityResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.Link
import com.stripe.android.paymentsheet.model.isLink
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkDisabledState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.ui.SepaMandateContract
import com.stripe.android.paymentsheet.ui.SepaMandateResult
import com.stripe.android.paymentsheet.utils.toConfirmationError
import com.stripe.android.uicore.utils.AnimationConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Named

@OptIn(WalletButtonsPreview::class)
@FlowControllerScope
internal class DefaultFlowController @Inject internal constructor(
    private val viewModelScope: CoroutineScope,
    private val lifecycleOwner: LifecycleOwner,
    private val paymentOptionFactory: PaymentOptionFactory,
    private val paymentOptionResultCallback: PaymentOptionResultCallback,
    private val paymentResultCallback: PaymentSheetResultCallback,
    activityResultCaller: ActivityResultCaller,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    // Properties provided through injection
    private val context: Context,
    private val eventReporter: EventReporter,
    private val viewModel: FlowControllerViewModel,
    private val confirmationHandler: FlowControllerConfirmationHandler,
    private val linkGateFactory: LinkGate.Factory,
    private val linkHandler: LinkHandler,
    private val linkAccountHolder: LinkAccountHolder,
    @Named(FLOW_CONTROLLER_LINK_LAUNCHER) private val flowControllerLinkLauncher: LinkPaymentLauncher,
    @Named(WALLETS_BUTTON_LINK_LAUNCHER) private val walletsButtonLinkLauncher: LinkPaymentLauncher,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
    private val configurationHandler: FlowControllerConfigurationHandler,
    private val errorReporter: ErrorReporter,
    @InitializedViaCompose private val initializedViaCompose: Boolean,
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper
) : PaymentSheet.FlowController {
    private val paymentOptionActivityLauncher: ActivityResultLauncher<PaymentOptionContract.Args>
    private val sepaMandateActivityLauncher: ActivityResultLauncher<SepaMandateContract.Args>

    /**
     * [FlowControllerComponent] is hold to inject into [Activity]s and created
     * after [DefaultFlowController].
     */
    lateinit var flowControllerComponent: FlowControllerComponent

    override var shippingDetails: AddressDetails?
        get() = viewModel.state?.config?.shippingDetails
        set(value) {
            val state = viewModel.state
            if (state != null) {
                viewModel.state = state.copy(
                    config = state.config.newBuilder()
                        .shippingDetails(value)
                        .build()
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

        flowControllerLinkLauncher.register(
            key = FLOW_CONTROLLER_LINK_LAUNCHER,
            activityResultRegistry = activityResultRegistryOwner.activityResultRegistry,
            callback = ::onLinkResultFromFlowController
        )

        walletsButtonLinkLauncher.register(
            key = WALLETS_BUTTON_LINK_LAUNCHER,
            activityResultRegistry = activityResultRegistryOwner.activityResultRegistry,
            callback = ::onLinkResultFromWalletsButton
        )

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    paymentOptionActivityLauncher.unregister()
                    sepaMandateActivityLauncher.unregister()
                    walletsButtonLinkLauncher.unregister()
                    flowControllerLinkLauncher.unregister()
                    PaymentElementCallbackReferences.remove(paymentElementCallbackIdentifier)
                }
            }
        )

        lifecycleOwner.lifecycleScope.launch {
            confirmationHandler.state.collect { state ->
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
        viewModel.flowControllerStateComponent.walletButtonsContent.Content(
            remember {
                WalletButtonsViewClickHandler { false }
            }
        )
    }

    @Composable
    override fun WalletButtons(clickHandler: WalletButtonsViewClickHandler) {
        viewModel.flowControllerStateComponent.walletButtonsContent.Content(clickHandler)
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

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun configureWithCheckout(
        checkout: Checkout,
        configuration: PaymentSheet.Configuration,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        CheckoutInstances.ensureNoMutationInFlight(checkout.internalState.key)
        configure(
            mode = checkout.internalState.initializationMode,
            configuration = CheckoutConfigurationMerger.PaymentSheetConfiguration(configuration)
                .forCheckoutSession(checkout.internalState),
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
            val checkoutSession = state.paymentSheetState.paymentMethodMetadata
                .integrationMetadata as? IntegrationMetadata.CheckoutSession
            if (checkoutSession != null) {
                CheckoutInstances.ensureNoMutationInFlight(checkoutSession.instancesKey)
                CheckoutInstances.markIntegrationLaunched(checkoutSession.instancesKey)
            }

            val linkConfiguration = state.paymentSheetState.linkConfiguration
            val paymentSelection = viewModel.paymentSelection
            val linkAccountInfo = linkAccountHolder.linkAccountInfo.value

            val shouldPresentLink = linkConfiguration != null && shouldPresentLinkInsteadOfPaymentOptions(
                declinedLink2FA = viewModel.state?.declinedLink2FA == true,
                paymentSelection = paymentSelection,
                linkAccountInfo = linkAccountInfo,
                linkGateFactory = linkGateFactory,
                linkConfiguration = linkConfiguration,
            )

            if (shouldPresentLink) {
                val paymentMethodMetadata = state.paymentSheetState.paymentMethodMetadata
                flowControllerLinkLauncher.present(
                    configuration = linkConfiguration,
                    paymentMethodMetadata = paymentMethodMetadata,
                    linkAccountInfo = linkAccountInfo,
                    linkExpressMode = LinkExpressMode.ENABLED,
                    launchMode = LinkLaunchMode.PaymentMethodSelection(
                        selectedPayment = (paymentSelection as? Link)?.selectedPayment?.details
                    ),
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
            walletButtonsRendered = viewModel.walletButtonsRendered,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            promotions = paymentMethodMessagePromotionsHelper.getPromotions()
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

    fun onLinkResultFromFlowController(result: LinkActivityResult) = handleFlowControllerLinkResult(
        result = result,
        viewModel = viewModel,
        linkAccountHolder = linkAccountHolder,
        paymentOptionFactory = paymentOptionFactory,
        paymentOptionResultCallback = paymentOptionResultCallback,
        withCurrentState = ::withCurrentState,
        showPaymentOptionList = ::showPaymentOptionList,
    )

    fun onLinkResultFromWalletsButton(result: LinkActivityResult) = handleWalletsButtonLinkResult(
        result = result,
        viewModel = viewModel,
        linkAccountHolder = linkAccountHolder,
        paymentOptionFactory = paymentOptionFactory,
        paymentOptionResultCallback = paymentOptionResultCallback,
        withCurrentState = ::withCurrentState,
        showPaymentOptionList = ::showPaymentOptionList,
    )

    override fun confirm() {
        val state = viewModel.state

        if (state == null) {
            val error = IllegalStateException(
                "FlowController must be successfully initialized " +
                    "using configureWithPaymentIntent(), configureWithSetupIntent() or " +
                    "configureWithIntentConfiguration() before calling confirm()."
            )
            onPaymentResult(PaymentResult.Failed(error))
            return
        }

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
            is Link,
            is PaymentSelection.GooglePay,
            is PaymentSelection.ExternalPaymentMethod,
            is PaymentSelection.CustomPaymentMethod,
            is PaymentSelection.New,
            is PaymentSelection.ShopPay,
            null -> confirmPaymentSelection(
                paymentSelection = paymentSelection,
                state = state.paymentSheetState,
            )
            is PaymentSelection.Saved -> confirmSavedPaymentMethod(
                paymentSelection = paymentSelection,
                state = state.paymentSheetState,
            )
        }
    }

    private fun confirmSavedPaymentMethod(
        paymentSelection: PaymentSelection.Saved,
        state: PaymentSheetState.Full,
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
            confirmPaymentSelection(paymentSelection, state)
        }
    }

    @VisibleForTesting
    fun confirmPaymentSelection(
        paymentSelection: PaymentSelection?,
        state: PaymentSheetState.Full,
    ) {
        viewModelScope.launch {
            val confirmationOption = paymentSelection?.toConfirmationOption(
                configuration = state.config,
                linkConfiguration = state.linkConfiguration,
                cardFundingFilter = state.paymentMethodMetadata.cardFundingFilter
            )

            confirmationOption?.let { option ->
                confirmationHandler.start(
                    arguments = ConfirmationHandler.Args(
                        confirmationOption = option,
                        paymentMethodMetadata = state.paymentMethodMetadata,
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
        result: PaymentOptionsActivityResult?
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
            is PaymentOptionsActivityResult.Succeeded -> {
                viewModel.paymentSelection = result.paymentSelection.also { it.hasAcknowledgedSepaMandate = true }
                onPaymentSelection(
                    paymentSelection = viewModel.paymentSelection,
                    canceled = false,
                    paymentOptionFactory = paymentOptionFactory,
                    paymentOptionResultCallback = paymentOptionResultCallback,
                )
            }
            null,
            is PaymentOptionsActivityResult.Canceled -> {
                viewModel.paymentSelection = result?.paymentSelection
                onPaymentSelection(
                    paymentSelection = viewModel.paymentSelection,
                    canceled = true,
                    paymentOptionFactory = paymentOptionFactory,
                    paymentOptionResultCallback = paymentOptionResultCallback,
                )
            }
        }
    }

    private fun onIntentResult(result: ConfirmationHandler.Result) {
        when (result) {
            is ConfirmationHandler.Result.Succeeded -> {
                viewModel.paymentSelection?.let { paymentSelection ->
                    eventReporter.onPaymentSuccess(
                        paymentSelection = paymentSelection,
                        deferredIntentConfirmationType = result.metadata[DeferredIntentConfirmationTypeKey],
                        intentId = result.intent.id,
                    )
                }

                onPaymentResult(
                    paymentResult = PaymentResult.Completed,
                    deferredIntentConfirmationType = result.metadata[DeferredIntentConfirmationTypeKey],
                    shouldLog = false,
                    shouldResetOnCompleted = result.completedFullPaymentFlow,
                    intentId = result.intent.id,
                )
            }
            is ConfirmationHandler.Result.Failed -> {
                val error = result.toConfirmationError()

                error?.let { confirmationError ->
                    viewModel.paymentSelection?.let { paymentSelection ->
                        eventReporter.onPaymentFailure(
                            paymentSelection = paymentSelection,
                            error = confirmationError
                        )
                    }
                }

                onPaymentResult(
                    paymentResult = PaymentResult.Failed(result.cause),
                    deferredIntentConfirmationType = null,
                    shouldLog = false,
                )
            }
            is ConfirmationHandler.Result.Canceled -> {
                handleCancellation(
                    canceled = result,
                    presentPaymentOptions = ::presentPaymentOptions,
                    onPaymentResult = { paymentResult ->
                        onPaymentResult(
                            paymentResult = paymentResult,
                            deferredIntentConfirmationType = null,
                            shouldLog = false,
                        )
                    }
                )
            }
        }
    }

    internal fun onPaymentResult(
        paymentResult: PaymentResult,
        deferredIntentConfirmationType: DeferredIntentConfirmationType? = null,
        shouldLog: Boolean = true,
        shouldResetOnCompleted: Boolean = true,
        intentId: String? = null,
    ) {
        if (shouldLog) {
            logPaymentResult(
                paymentResult = paymentResult,
                paymentSelection = viewModel.paymentSelection,
                deferredIntentConfirmationType = deferredIntentConfirmationType,
                intentId = intentId,
                eventReporter = eventReporter,
            )
        }

        val selection = viewModel.paymentSelection

        if (shouldLogOutFromLink(paymentResult, selection, viewModel.state?.linkConfiguration)) {
            linkHandler.logOut()
        }

        if (paymentResult is PaymentResult.Completed && shouldResetOnCompleted) {
            viewModel.paymentSelection = null
            viewModel.state = null
            viewModel.previousConfigureRequest = null
            paymentOptionResultCallback.onPaymentOptionResult(PaymentOptionResult(null, false))
        }

        viewModelScope.launch {
            paymentResultCallback.onPaymentSheetResult(
                paymentResult.convertToPaymentSheetResult()
            )
        }
    }

    internal fun onSepaMandateResult(sepaMandateResult: SepaMandateResult) = onSepaMandateResult(
        sepaMandateResult = sepaMandateResult,
        viewModel = viewModel,
        confirm = ::confirm,
        paymentResultCallback = paymentResultCallback,
    )

    @Parcelize
    data class Args(
        val clientSecret: String,
        val config: PaymentSheet.Configuration?
    ) : Parcelable

    @Parcelize
    data class State(
        val paymentSheetState: PaymentSheetState.Full,
        val config: PaymentSheet.Configuration,
        val declinedLink2FA: Boolean = false
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
        internal const val FLOW_CONTROLLER_LINK_LAUNCHER = "LinkPaymentLauncher_DefaultFlowController"
        internal const val WALLETS_BUTTON_LINK_LAUNCHER = "LinkPaymentLauncher_WalletsButton"

        fun getInstance(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            activityResultCaller: ActivityResultCaller,
            statusBarColor: () -> Int?,
            paymentOptionResultCallback: PaymentOptionResultCallback,
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
                flowControllerStateComponent.flowControllerComponentFactory
                    .create(
                        lifecycleOwner = lifecycleOwner,
                        activityResultCaller = activityResultCaller,
                        activityResultRegistryOwner = activityResultRegistryOwner,
                        paymentOptionResultCallback = paymentOptionResultCallback,
                        paymentResultCallback = paymentResultCallback,
                        initializedViaCompose = initializedViaCompose,
                    )
            val flowController = flowControllerComponent.flowController
            flowController.flowControllerComponent = flowControllerComponent
            return flowController
        }
    }
}

private fun shouldPresentLinkInsteadOfPaymentOptions(
    declinedLink2FA: Boolean,
    paymentSelection: PaymentSelection?,
    linkAccountInfo: LinkAccountUpdate.Value,
    linkGateFactory: LinkGate.Factory,
    linkConfiguration: LinkConfiguration,
): Boolean {
    return !declinedLink2FA &&
        paymentSelection is Link &&
        linkAccountInfo.account != null &&
        linkGateFactory.create(linkConfiguration).showRuxInFlowController
}

private fun handleFlowControllerLinkResult(
    result: LinkActivityResult,
    viewModel: FlowControllerViewModel,
    linkAccountHolder: LinkAccountHolder,
    paymentOptionFactory: PaymentOptionFactory,
    paymentOptionResultCallback: PaymentOptionResultCallback,
    withCurrentState: ((DefaultFlowController.State) -> Unit) -> Unit,
    showPaymentOptionList: (DefaultFlowController.State, PaymentSelection?) -> Unit,
) {
    result.linkAccountUpdate?.updateLinkAccount(viewModel, linkAccountHolder)
    when (result) {
        is LinkActivityResult.PaymentMethodObtained,
        is LinkActivityResult.Failed -> Unit
        is LinkActivityResult.Canceled -> when (result.reason) {
            Reason.BackPressed -> withCurrentState { state ->
                val accountStatus = linkAccountHolder.linkAccountInfo.value.account?.accountStatus
                if (accountStatus == AccountStatus.VerificationStarted) {
                    viewModel.updateState { it?.copy(declinedLink2FA = true) }
                }
                if (viewModel.paymentSelection?.readyToPayWithLink() == false) {
                    showPaymentOptionList(state, viewModel.paymentSelection)
                }
            }
            Reason.LoggedOut -> {
                updateLinkPaymentSelection(
                    viewModel = viewModel,
                    linkPaymentMethod = null,
                    canceled = true,
                    paymentOptionFactory = paymentOptionFactory,
                    paymentOptionResultCallback = paymentOptionResultCallback,
                )
                withCurrentState { showPaymentOptionList(it, viewModel.paymentSelection) }
            }
            Reason.PayAnotherWay -> {
                withCurrentState { showPaymentOptionList(it, viewModel.paymentSelection) }
            }
        }
        is LinkActivityResult.Completed -> {
            updateLinkPaymentSelection(
                viewModel = viewModel,
                linkPaymentMethod = result.selectedPayment,
                canceled = false,
                paymentOptionFactory = paymentOptionFactory,
                paymentOptionResultCallback = paymentOptionResultCallback,
            )
        }
    }
}

private fun handleWalletsButtonLinkResult(
    result: LinkActivityResult,
    viewModel: FlowControllerViewModel,
    linkAccountHolder: LinkAccountHolder,
    paymentOptionFactory: PaymentOptionFactory,
    paymentOptionResultCallback: PaymentOptionResultCallback,
    withCurrentState: ((DefaultFlowController.State) -> Unit) -> Unit,
    showPaymentOptionList: (DefaultFlowController.State, PaymentSelection?) -> Unit,
) {
    result.linkAccountUpdate?.updateLinkAccount(viewModel, linkAccountHolder)
    viewModel.flowControllerStateComponent.linkInlineInteractor.onLinkResult()
    when (result) {
        is LinkActivityResult.PaymentMethodObtained,
        is LinkActivityResult.Failed -> Unit
        is LinkActivityResult.Canceled -> when (result.reason) {
            Reason.BackPressed -> Unit
            Reason.LoggedOut -> {
                updateLinkPaymentSelection(
                    viewModel = viewModel,
                    linkPaymentMethod = null,
                    canceled = true,
                    paymentOptionFactory = paymentOptionFactory,
                    paymentOptionResultCallback = paymentOptionResultCallback,
                )
            }
            Reason.PayAnotherWay -> withCurrentState {
                showPaymentOptionList(it, viewModel.paymentSelection)
            }
        }
        is LinkActivityResult.Completed -> with(
            Link(
                linkBrand = viewModel.state?.paymentSheetState?.paymentMethodMetadata?.linkBrandOrDefault
                    ?: LinkBrand.Link,
                selectedPayment = result.selectedPayment,
            )
        ) {
            viewModel.paymentSelection = this
            paymentOptionResultCallback.onPaymentOptionResult(
                PaymentOptionResult(
                    paymentOption = paymentOptionFactory.create(this),
                    didCancel = false,
                )
            )
        }
    }
}

private fun PaymentSelection.readyToPayWithLink(): Boolean = when (this) {
    is Link -> selectedPayment != null
    else -> isLink
}

private fun LinkAccountUpdate.updateLinkAccount(
    viewModel: FlowControllerViewModel,
    linkAccountHolder: LinkAccountHolder,
) {
    updateLinkAccount(linkAccountHolder)
    when (this) {
        is LinkAccountUpdate.Value -> {
            val currentState = viewModel.state ?: return
            val metadata = currentState.paymentSheetState.paymentMethodMetadata
            val accountStatus = account?.accountStatus ?: AccountStatus.SignedOut
            val linkStateResult = when (val result = metadata.linkStateResult) {
                is LinkState -> result.copy(loginState = accountStatus.toLoginState())
                is LinkDisabledState, null -> result
            }
            val updatedMetadata = metadata.copy(linkStateResult = linkStateResult)
            viewModel.state = currentState.copyPaymentSheetState(metadata = updatedMetadata)
        }
        LinkAccountUpdate.None -> Unit
    }
}

private fun updateLinkPaymentSelection(
    viewModel: FlowControllerViewModel,
    linkPaymentMethod: LinkPaymentMethod?,
    canceled: Boolean,
    paymentOptionFactory: PaymentOptionFactory,
    paymentOptionResultCallback: PaymentOptionResultCallback,
) {
    val paymentSelection = viewModel.paymentSelection
    if (paymentSelection is Link) {
        val newSelection = if (linkPaymentMethod != null) {
            paymentSelection.copy(selectedPayment = linkPaymentMethod)
        } else {
            viewModel.state?.paymentSheetState?.determineFallbackPaymentSelectionAfterLinkLogout()
        }
        viewModel.paymentSelection = newSelection
        val paymentOption = newSelection?.let { paymentOptionFactory.create(it) }
        paymentOptionResultCallback.onPaymentOptionResult(
            PaymentOptionResult(
                paymentOption = paymentOption,
                didCancel = canceled,
            )
        )
    }
}

private fun onPaymentSelection(
    paymentSelection: PaymentSelection?,
    canceled: Boolean,
    paymentOptionFactory: PaymentOptionFactory,
    paymentOptionResultCallback: PaymentOptionResultCallback,
) {
    val paymentOption = paymentSelection?.let { paymentOptionFactory.create(it) }
    paymentOptionResultCallback.onPaymentOptionResult(
        PaymentOptionResult(
            paymentOption = paymentOption,
            didCancel = canceled,
        )
    )
}

private fun handleCancellation(
    canceled: ConfirmationHandler.Result.Canceled,
    presentPaymentOptions: () -> Unit,
    onPaymentResult: (PaymentResult) -> Unit,
) {
    when (canceled.action) {
        ConfirmationHandler.Result.Canceled.Action.InformCancellation -> {
            onPaymentResult(PaymentResult.Canceled)
        }
        ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails -> presentPaymentOptions()
        ConfirmationHandler.Result.Canceled.Action.None -> Unit
    }
}

private fun onSepaMandateResult(
    sepaMandateResult: SepaMandateResult,
    viewModel: FlowControllerViewModel,
    confirm: () -> Unit,
    paymentResultCallback: PaymentSheetResultCallback,
) {
    when (sepaMandateResult) {
        SepaMandateResult.Acknowledged -> {
            viewModel.paymentSelection?.hasAcknowledgedSepaMandate = true
            confirm()
        }
        SepaMandateResult.Canceled -> {
            paymentResultCallback.onPaymentSheetResult(PaymentSheetResult.Canceled())
        }
    }
}

private fun shouldLogOutFromLink(
    paymentResult: PaymentResult,
    selection: PaymentSelection?,
    linkConfiguration: LinkConfiguration?,
): Boolean {
    val verifiedMerchant = linkConfiguration?.useAttestationEndpointsForLink == true
    return paymentResult is PaymentResult.Completed && selection != null &&
        selection.isLink &&
        verifiedMerchant.not()
}

private fun logPaymentResult(
    paymentResult: PaymentResult?,
    paymentSelection: PaymentSelection?,
    deferredIntentConfirmationType: DeferredIntentConfirmationType?,
    intentId: String?,
    eventReporter: EventReporter,
) {
    when (paymentResult) {
        is PaymentResult.Completed -> {
            paymentSelection?.let { selection ->
                eventReporter.onPaymentSuccess(
                    paymentSelection = selection,
                    deferredIntentConfirmationType = deferredIntentConfirmationType,
                    intentId = intentId,
                )
            }
        }
        is PaymentResult.Failed -> {
            paymentSelection?.let { selection ->
                eventReporter.onPaymentFailure(
                    paymentSelection = selection,
                    error = PaymentSheetConfirmationError.Stripe(paymentResult.throwable),
                )
            }
        }
        else -> Unit
    }
}

private fun PaymentResult.convertToPaymentSheetResult() = when (this) {
    is PaymentResult.Completed -> PaymentSheetResult.Completed()
    is PaymentResult.Canceled -> PaymentSheetResult.Canceled()
    is PaymentResult.Failed -> PaymentSheetResult.Failed(throwable)
}
