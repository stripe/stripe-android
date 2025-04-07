package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultCaller
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.analytics.SessionSavedStateHandler
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.toConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.analytics.primaryButtonColorUsage
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.injection.DaggerPaymentSheetLauncherComponent
import com.stripe.android.paymentsheet.injection.PaymentSheetViewModelModule
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.isLink
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.Args
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcCompletionState
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionInteractor
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.DefaultSelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.utils.canSave
import com.stripe.android.paymentsheet.utils.toConfirmationError
import com.stripe.android.paymentsheet.verticalmode.VerticalModeInitialScreenFactory
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.PrimaryButtonUiStateMapper
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetViewModel @Inject internal constructor(
    // Properties provided through PaymentSheetViewModelComponent.Builder
    internal val args: PaymentSheetContractV2.Args,
    eventReporter: EventReporter,
    private val paymentElementLoader: PaymentElementLoader,
    customerRepository: CustomerRepository,
    private val prefsRepository: PrefsRepository,
    private val logger: Logger,
    @IOContext workContext: CoroutineContext,
    savedStateHandle: SavedStateHandle,
    linkHandler: LinkHandler,
    confirmationHandlerFactory: ConfirmationHandler.Factory,
    cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val errorReporter: ErrorReporter,
    internal val cvcRecollectionHandler: CvcRecollectionHandler,
    private val cvcRecollectionInteractorFactory: CvcRecollectionInteractor.Factory
) : BaseSheetViewModel(
    config = args.config,
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    workContext = workContext,
    savedStateHandle = savedStateHandle,
    linkHandler = linkHandler,
    cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
    isCompleteFlow = true,
) {

    private val _contentVisible = MutableStateFlow(true)
    internal val contentVisible: StateFlow<Boolean> = _contentVisible

    private val primaryButtonUiStateMapper = PrimaryButtonUiStateMapper(
        config = config,
        isProcessingPayment = isProcessingPaymentIntent,
        currentScreenFlow = navigationHandler.currentScreen,
        buttonsEnabledFlow = buttonsEnabled,
        amountFlow = paymentMethodMetadata.mapAsStateFlow { it?.amount() },
        selectionFlow = selection,
        customPrimaryButtonUiStateFlow = customPrimaryButtonUiState,
        cvcCompleteFlow = cvcRecollectionCompleteFlow,
        onClick = {
            eventReporter.onPressConfirmButton(selection.value)
            checkout()
        },
    )

    private val _paymentSheetResult = MutableSharedFlow<PaymentSheetResult>(replay = 1)
    internal val paymentSheetResult: SharedFlow<PaymentSheetResult> = _paymentSheetResult

    @VisibleForTesting
    internal val viewState = MutableStateFlow<PaymentSheetViewState?>(null)

    internal var checkoutIdentifier: CheckoutIdentifier = CheckoutIdentifier.SheetBottomBuy

    val buyButtonState: StateFlow<PaymentSheetViewState?> = viewState.mapAsStateFlow { viewState ->
        mapViewStateToCheckoutIdentifier(viewState, CheckoutIdentifier.SheetBottomBuy)
    }

    internal val isProcessingPaymentIntent
        get() = args.initializationMode.isProcessingPayment

    private var inProgressSelection: PaymentSelection?
        get() = savedStateHandle[IN_PROGRESS_SELECTION]
        set(value) {
            savedStateHandle[IN_PROGRESS_SELECTION] = value
        }

    override var newPaymentSelection: NewPaymentOptionSelection? = null

    private val googlePayButtonType: GooglePayButtonType =
        when (args.config.googlePay?.buttonType) {
            PaymentSheet.GooglePayConfiguration.ButtonType.Buy -> GooglePayButtonType.Buy
            PaymentSheet.GooglePayConfiguration.ButtonType.Book -> GooglePayButtonType.Book
            PaymentSheet.GooglePayConfiguration.ButtonType.Checkout -> GooglePayButtonType.Checkout
            PaymentSheet.GooglePayConfiguration.ButtonType.Donate -> GooglePayButtonType.Donate
            PaymentSheet.GooglePayConfiguration.ButtonType.Order -> GooglePayButtonType.Order
            PaymentSheet.GooglePayConfiguration.ButtonType.Subscribe -> GooglePayButtonType.Subscribe
            PaymentSheet.GooglePayConfiguration.ButtonType.Plain -> GooglePayButtonType.Plain
            PaymentSheet.GooglePayConfiguration.ButtonType.Pay,
            null -> GooglePayButtonType.Pay
        }

    @VisibleForTesting
    internal val googlePayLauncherConfig: GooglePayPaymentMethodLauncher.Config? =
        args.googlePayConfig?.let { config ->
            if (config.currencyCode == null && !isProcessingPaymentIntent) {
                logger.warning(
                    "GooglePayConfiguration.currencyCode is required in order to use " +
                        "Google Pay when processing a Setup Intent"
                )
                null
            } else {
                GooglePayPaymentMethodLauncher.Config(
                    environment = when (config.environment) {
                        PaymentSheet.GooglePayConfiguration.Environment.Production ->
                            GooglePayEnvironment.Production
                        else ->
                            GooglePayEnvironment.Test
                    },
                    merchantCountryCode = config.countryCode,
                    merchantName = this.config.merchantDisplayName,
                    isEmailRequired = args.config.billingDetailsCollectionConfiguration.collectsEmail,
                    billingAddressConfig = args.config.billingDetailsCollectionConfiguration.toBillingAddressConfig(),
                )
            }
        }

    override val primaryButtonUiState = primaryButtonUiStateMapper.forCompleteFlow()

    override val error: StateFlow<ResolvableString?> = buyButtonState.mapAsStateFlow { it?.errorMessage?.message }

    override val walletsState: StateFlow<WalletsState?> = combineAsStateFlow(
        linkHandler.isLinkEnabled,
        linkHandler.linkConfigurationCoordinator.emailFlow,
        buttonsEnabled,
        paymentMethodMetadata,
    ) { isLinkAvailable, linkEmail, buttonsEnabled, paymentMethodMetadata ->
        WalletsState.create(
            isLinkAvailable = isLinkAvailable,
            linkEmail = linkEmail,
            isGooglePayReady = paymentMethodMetadata?.isGooglePayReady == true,
            buttonsEnabled = buttonsEnabled,
            paymentMethodTypes = paymentMethodMetadata?.supportedPaymentMethodTypes().orEmpty(),
            googlePayLauncherConfig = googlePayLauncherConfig,
            googlePayButtonType = googlePayButtonType,
            onGooglePayPressed = this::checkoutWithGooglePay,
            onLinkPressed = this::checkoutWithLink,
            isSetupIntent = paymentMethodMetadata?.stripeIntent is SetupIntent
        )
    }

    override val walletsProcessingState: StateFlow<WalletsProcessingState?> = viewState.mapAsStateFlow { vs ->
        when (val viewState = mapViewStateToCheckoutIdentifier(vs, CheckoutIdentifier.SheetTopWallet)) {
            null -> null
            is PaymentSheetViewState.Reset -> WalletsProcessingState.Idle(
                error = viewState.errorMessage?.message
            )
            is PaymentSheetViewState.StartProcessing -> WalletsProcessingState.Processing
            is PaymentSheetViewState.FinishProcessing -> WalletsProcessingState.Completed(viewState.onComplete)
        }
    }

    private val confirmationHandler = confirmationHandlerFactory.create(viewModelScope.plus(workContext))

    init {
        SessionSavedStateHandler.attachTo(this, savedStateHandle)

        val isDeferred = args.initializationMode is PaymentElementLoader.InitializationMode.DeferredIntent

        eventReporter.onInit(
            commonConfiguration = config.asCommonConfiguration(),
            primaryButtonColor = config.primaryButtonColorUsage(),
            paymentMethodLayout = config.paymentMethodLayout,
            isDeferred = isDeferred,
            appearance = config.appearance
        )

        viewModelScope.launch(workContext) {
            loadPaymentSheetState()
        }
    }

    private suspend fun loadPaymentSheetState() {
        val result = withContext(workContext) {
            paymentElementLoader.load(
                initializationMode = args.initializationMode,
                configuration = args.config.asCommonConfiguration(),
                isReloadingAfterProcessDeath = confirmationHandler.hasReloadedFromProcessDeath,
                initializedViaCompose = args.initializedViaCompose,
            )
        }

        result.fold(
            onSuccess = { handlePaymentSheetStateLoaded(PaymentSheetState.Full(it)) },
            onFailure = { handlePaymentSheetStateLoadFailure(it) },
        )
    }

    private fun handlePaymentSheetStateLoadFailure(error: Throwable) {
        setPaymentMethodMetadata(null)
        onFatal(error)
    }

    private suspend fun handlePaymentSheetStateLoaded(state: PaymentSheetState.Full) {
        val pendingResult = confirmationHandler.awaitResult()

        if (pendingResult is ConfirmationHandler.Result.Succeeded) {
            // If we just received a transaction result after process death, we don't error. Instead, we dismiss
            // PaymentSheet and return a `Completed` result to the caller.
            handlePaymentCompleted(
                intent = pendingResult.intent,
                deferredIntentConfirmationType = pendingResult.deferredIntentConfirmationType,
                finishImmediately = true
            )
        } else if (state.validationError != null) {
            handlePaymentSheetStateLoadFailure(state.validationError)
        } else {
            initializeWithState(state)
        }
    }

    private suspend fun initializeWithState(state: PaymentSheetState.Full) {
        withContext(Dispatchers.Main.immediate) {
            customerStateHolder.setCustomerState(state.customer)

            updateSelection(state.paymentSelection)

            setPaymentMethodMetadata(state.paymentMethodMetadata)

            val shouldLaunchEagerly = linkHandler.setupLinkWithEagerLaunch(state.paymentMethodMetadata.linkState)

            val pendingFailedPaymentResult = confirmationHandler.awaitResult()
                as? ConfirmationHandler.Result.Failed
            val errorMessage = pendingFailedPaymentResult?.cause?.stripeErrorMessage()

            resetViewState(errorMessage)
            navigationHandler.resetTo(
                determineInitialBackStack(
                    paymentMethodMetadata = state.paymentMethodMetadata,
                    customerStateHolder = customerStateHolder,
                )
            )

            if (shouldLaunchEagerly) {
                checkoutWithLinkExpress()
            }
        }

        viewModelScope.launch {
            confirmationHandler.state.collectLatest { state ->
                when (state) {
                    is ConfirmationHandler.State.Idle -> Unit
                    is ConfirmationHandler.State.Confirming -> {
                        if (state.option is GooglePayConfirmationOption) {
                            setContentVisible(false)
                        } else {
                            setContentVisible(true)
                        }

                        startProcessing(checkoutIdentifier)

                        if (viewState.value !is PaymentSheetViewState.StartProcessing) {
                            startProcessing(checkoutIdentifier)
                        }
                    }
                    is ConfirmationHandler.State.Complete -> {
                        setContentVisible(true)
                        processConfirmationResult(state.result)
                    }
                }
            }
        }
    }

    private fun resetViewState(userErrorMessage: ResolvableString? = null) {
        viewState.value =
            PaymentSheetViewState.Reset(userErrorMessage?.let { PaymentSheetViewState.UserErrorMessage(it) })
        savedStateHandle[SAVE_PROCESSING] = false
    }

    private fun startProcessing(checkoutIdentifier: CheckoutIdentifier) {
        this.checkoutIdentifier = checkoutIdentifier
        savedStateHandle[SAVE_PROCESSING] = true
        viewState.value = PaymentSheetViewState.StartProcessing
    }

    fun checkout() {
        val currentSelection = selection.value

        if (currentSelection is PaymentSelection.Saved && shouldLaunchCvcRecollectionScreen(currentSelection)) {
            launchCvcRecollection(currentSelection)
            return
        }
        checkout(currentSelection, CheckoutIdentifier.SheetBottomBuy)
    }

    fun checkoutWithGooglePay() {
        checkout(PaymentSelection.GooglePay, CheckoutIdentifier.SheetTopWallet)
    }

    fun checkoutWithLink() {
        checkout(PaymentSelection.Link(useLinkExpress = false), CheckoutIdentifier.SheetTopWallet)
    }

    private fun checkoutWithLinkExpress() {
        checkout(PaymentSelection.Link(useLinkExpress = true), CheckoutIdentifier.SheetTopWallet)
    }

    private fun checkout(
        paymentSelection: PaymentSelection?,
        identifier: CheckoutIdentifier,
    ) {
        this.checkoutIdentifier = identifier

        confirmPaymentSelection(paymentSelection)
    }

    override fun handlePaymentMethodSelected(selection: PaymentSelection?) {
        if (selection != this.selection.value) {
            updateSelection(selection)
        }
    }

    override fun clearErrorMessages() {
        if (viewState.value is PaymentSheetViewState.Reset) {
            viewState.value = PaymentSheetViewState.Reset(message = null)
        }
    }

    private fun launchCvcRecollection(selection: PaymentSelection.Saved) {
        cvcRecollectionHandler.launch(selection.paymentMethod) { cvcRecollectionData ->
            val interactor = cvcRecollectionInteractorFactory.create(
                args = Args(
                    lastFour = cvcRecollectionData.lastFour ?: "",
                    cardBrand = cvcRecollectionData.brand,
                    cvc = "",
                    isTestMode = paymentMethodMetadata.value?.stripeIntent?.isLiveMode?.not() ?: false,
                ),
                processing = processing,
                coroutineScope = viewModelScope,
            )
            viewModelScope.launch {
                interactor.cvcCompletionState.collectLatest(::handleCvcCompletionState)
            }
            navigationHandler.transitionTo(PaymentSheetScreen.CvcRecollection(interactor))
        }
    }

    private fun handleCvcCompletionState(completionState: CvcCompletionState) {
        (selection.value as? PaymentSelection.Saved)?.let {
            val paymentMethodOptionsParams = when (completionState) {
                is CvcCompletionState.Completed -> {
                    PaymentMethodOptionsParams.Card(
                        cvc = completionState.cvc,
                    )
                }
                CvcCompletionState.Incomplete -> {
                    PaymentMethodOptionsParams.Card(
                        cvc = "",
                    )
                }
            }
            updateSelection(
                selection = PaymentSelection.Saved(
                    paymentMethod = it.paymentMethod,
                    walletType = it.walletType,
                    paymentMethodOptionsParams = paymentMethodOptionsParams
                )
            )
        }
    }

    /**
     * Used to set up any dependencies that require a reference to the current Activity.
     * Must be called from the Activity's `onCreate`.
     */
    fun registerFromActivity(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
    }

    @Suppress("ComplexCondition")
    private fun paymentSelectionWithCvcIfEnabled(paymentSelection: PaymentSelection?): PaymentSelection? {
        if (paymentSelection !is PaymentSelection.Saved) return paymentSelection
        return if (shouldAttachCvc(paymentSelection)) {
            val paymentMethodOptionsParams =
                (paymentSelection.paymentMethodOptionsParams as? PaymentMethodOptionsParams.Card)
                    ?: PaymentMethodOptionsParams.Card()
            val newSelection = paymentSelection.copy(
                paymentMethodOptionsParams = paymentMethodOptionsParams.copy(
                    cvc = cvcControllerFlow.value.fieldValue.value
                )
            )
            updateSelection(newSelection)
            newSelection
        } else {
            paymentSelection
        }
    }

    private fun confirmPaymentSelection(paymentSelection: PaymentSelection?) {
        viewModelScope.launch(workContext) {
            val confirmationOption = withContext(viewModelScope.coroutineContext) {
                inProgressSelection = paymentSelection

                paymentSelectionWithCvcIfEnabled(paymentSelection)
                    ?.toConfirmationOption(
                        configuration = config.asCommonConfiguration(),
                        linkConfiguration = linkHandler.linkConfiguration.value,
                    )
            }

            confirmationOption?.let { option ->
                val stripeIntent = awaitStripeIntent()

                confirmationHandler.start(
                    arguments = ConfirmationHandler.Args(
                        intent = stripeIntent,
                        confirmationOption = option,
                        initializationMode = args.initializationMode,
                        appearance = config.appearance,
                        shippingDetails = config.shippingDetails,
                    ),
                )
            } ?: run {
                inProgressSelection = null

                val message = paymentSelection?.let {
                    "Cannot confirm using a ${it::class.qualifiedName} payment selection!"
                } ?: "Cannot confirm without a payment selection!"

                val exception = IllegalStateException(message)

                val event = paymentSelection?.let {
                    ErrorReporter.UnexpectedErrorEvent.PAYMENT_SHEET_INVALID_PAYMENT_SELECTION_ON_CHECKOUT
                } ?: ErrorReporter.UnexpectedErrorEvent.PAYMENT_SHEET_NO_PAYMENT_SELECTION_ON_CHECKOUT

                errorReporter.report(event, StripeException.create(exception))

                processConfirmationResult(
                    ConfirmationHandler.Result.Failed(
                        cause = exception,
                        message = exception.stripeErrorMessage(),
                        type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
                    )
                )
            }
        }
    }

    private fun handlePaymentFailed(
        error: PaymentSheetConfirmationError,
        message: ResolvableString
    ) {
        eventReporter.onPaymentFailure(
            paymentSelection = inProgressSelection,
            error = error,
        )

        resetViewState(
            userErrorMessage = message
        )
    }

    private fun handlePaymentCompleted(
        intent: StripeIntent,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        finishImmediately: Boolean
    ) {
        val currentSelection = inProgressSelection
        eventReporter.onPaymentSuccess(
            paymentSelection = currentSelection,
            deferredIntentConfirmationType = deferredIntentConfirmationType,
        )

        // Log out of Link to invalidate the token
        if (currentSelection != null && currentSelection.isLink) {
            linkHandler.logOut()
        }

        /*
         * Sets current selection as default payment method in future payment sheet usage. New payment
         * methods are only saved if the payment sheet is in setup mode, is in payment intent with setup
         * for usage, or the customer has requested the payment method be saved.
         */
        when (currentSelection) {
            is PaymentSelection.New -> intent.paymentMethod.takeIf {
                currentSelection.canSave(args.initializationMode)
            }?.let { method ->
                PaymentSelection.Saved(method)
            }
            else -> currentSelection
        }?.let {
            prefsRepository.savePaymentSelection(it)
        }

        inProgressSelection = null

        if (finishImmediately) {
            _paymentSheetResult.tryEmit(PaymentSheetResult.Completed)
        } else {
            viewState.value = PaymentSheetViewState.FinishProcessing {
                _paymentSheetResult.tryEmit(PaymentSheetResult.Completed)
            }
        }
    }

    private fun processConfirmationResult(result: ConfirmationHandler.Result?) {
        when (result) {
            is ConfirmationHandler.Result.Succeeded -> handlePaymentCompleted(
                intent = result.intent,
                deferredIntentConfirmationType = result.deferredIntentConfirmationType,
                finishImmediately = false,
            )
            is ConfirmationHandler.Result.Failed -> processConfirmationFailure(result)
            is ConfirmationHandler.Result.Canceled,
            null -> resetViewState()
        }

        inProgressSelection = null
    }

    private fun processConfirmationFailure(failure: ConfirmationHandler.Result.Failed) {
        when (failure.type) {
            ConfirmationHandler.Result.Failed.ErrorType.Payment,
            ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod,
            is ConfirmationHandler.Result.Failed.ErrorType.GooglePay -> {
                failure.toConfirmationError()?.let { confirmationError ->
                    handlePaymentFailed(
                        error = confirmationError,
                        message = failure.message,
                    )
                }
            }
            ConfirmationHandler.Result.Failed.ErrorType.Fatal -> onFatal(failure.cause)
            ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration,
            ConfirmationHandler.Result.Failed.ErrorType.Internal -> onError(failure.message)
        }
    }

    private fun onFatal(throwable: Throwable) {
        logger.error("Payment Sheet error", throwable)
        _paymentSheetResult.tryEmit(PaymentSheetResult.Failed(throwable))
    }

    override fun onUserCancel() {
        eventReporter.onDismiss()
        _paymentSheetResult.tryEmit(PaymentSheetResult.Canceled)
    }

    override fun onError(error: ResolvableString?) = resetViewState(error)

    private fun determineInitialBackStack(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerStateHolder: CustomerStateHolder,
    ): List<PaymentSheetScreen> {
        if (config.paymentMethodLayout != PaymentSheet.PaymentMethodLayout.Horizontal) {
            return VerticalModeInitialScreenFactory.create(
                viewModel = this,
                paymentMethodMetadata = paymentMethodMetadata,
                customerStateHolder = customerStateHolder,
            )
        }
        val hasPaymentMethods = customerStateHolder.paymentMethods.value.isNotEmpty()
        val target = if (hasPaymentMethods) {
            val interactor = DefaultSelectSavedPaymentMethodsInteractor.create(
                viewModel = this,
                paymentMethodMetadata = paymentMethodMetadata,
                customerStateHolder = customerStateHolder,
                savedPaymentMethodMutator = savedPaymentMethodMutator,
            )
            PaymentSheetScreen.SelectSavedPaymentMethods(
                interactor = interactor,
                cvcRecollectionState = getCvcRecollectionState()
            )
        } else {
            val interactor = DefaultAddPaymentMethodInteractor.create(
                viewModel = this,
                paymentMethodMetadata = paymentMethodMetadata,
            )
            PaymentSheetScreen.AddFirstPaymentMethod(interactor = interactor)
        }
        return listOf(target)
    }

    private suspend fun awaitStripeIntent(): StripeIntent {
        return paymentMethodMetadata.filterNotNull().first().stripeIntent
    }

    internal fun getCvcRecollectionState(): PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState {
        return if (isCvcRecollectionEnabled()) {
            PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState.Required(cvcControllerFlow)
        } else {
            PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState.NotRequired
        }
    }

    private fun mapViewStateToCheckoutIdentifier(
        viewState: PaymentSheetViewState?,
        checkoutIdentifier: CheckoutIdentifier
    ): PaymentSheetViewState? {
        return if (this.checkoutIdentifier != checkoutIdentifier) {
            null
        } else {
            viewState
        }
    }

    private fun setContentVisible(visible: Boolean) {
        _contentVisible.value = visible
    }

    internal class Factory(
        private val starterArgsSupplier: () -> PaymentSheetContractV2.Args,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()
            val arguments = starterArgsSupplier()

            val component = DaggerPaymentSheetLauncherComponent
                .builder()
                .application(application)
                .savedStateHandle(savedStateHandle)
                .paymentElementCallbackIdentifier(arguments.paymentElementCallbackIdentifier)
                .build()
                .paymentSheetViewModelSubcomponentBuilder
                .paymentSheetViewModelModule(PaymentSheetViewModelModule(arguments))
                .build()

            return component.viewModel as T
        }
    }

    /**
     * This is the identifier of the caller of the [checkout] function.  It is used in
     * the observables of [viewState] to get state events related to it.
     */
    internal enum class CheckoutIdentifier {
        SheetTopWallet,
        SheetBottomBuy,
        None
    }

    private companion object {
        const val IN_PROGRESS_SELECTION = "IN_PROGRESS_PAYMENT_SELECTION"
    }
}

private val PaymentElementLoader.InitializationMode.isProcessingPayment: Boolean
    get() = when (this) {
        is PaymentElementLoader.InitializationMode.PaymentIntent -> true
        is PaymentElementLoader.InitializationMode.SetupIntent -> false
        is PaymentElementLoader.InitializationMode.DeferredIntent -> {
            intentConfiguration.mode is PaymentSheet.IntentConfiguration.Mode.Payment
        }
    }
