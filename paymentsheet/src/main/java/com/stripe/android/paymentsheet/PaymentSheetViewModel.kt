package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultCaller
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
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
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
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
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.utils.canSave
import com.stripe.android.paymentsheet.verticalmode.VerticalModeInitialScreenFactory
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.PrimaryButtonUiStateMapper
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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
    intentConfirmationHandlerFactory: IntentConfirmationHandler.Factory,
    cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    editInteractorFactory: ModifiableEditPaymentMethodViewInteractor.Factory,
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
    editInteractorFactory = editInteractorFactory,
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

    override var newPaymentSelection: NewOrExternalPaymentSelection? = null

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

    override val primaryButtonUiState = primaryButtonUiStateMapper.forCompleteFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null,
    )

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
            onLinkPressed = linkHandler::launchLink,
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

    private val intentConfirmationHandler = intentConfirmationHandlerFactory.create(viewModelScope.plus(workContext))

    init {
        SessionSavedStateHandler.attachTo(this, savedStateHandle)

        viewModelScope.launch {
            linkHandler.processingState.collect { processingState ->
                handleLinkProcessingState(processingState)
            }
        }

        val isDeferred = args.initializationMode is PaymentSheet.InitializationMode.DeferredIntent

        eventReporter.onInit(
            configuration = config,
            isDeferred = isDeferred,
        )

        viewModelScope.launch(workContext) {
            loadPaymentSheetState()
        }
    }

    private fun handleLinkProcessingState(processingState: LinkHandler.ProcessingState) {
        when (processingState) {
            LinkHandler.ProcessingState.Cancelled -> {
                resetViewState()
            }
            is LinkHandler.ProcessingState.PaymentMethodCollected -> {
                updateSelection(
                    PaymentSelection.Saved(
                        paymentMethod = processingState.paymentMethod,
                        walletType = PaymentSelection.Saved.WalletType.Link,
                    )
                )
                checkout(selection.value, CheckoutIdentifier.SheetTopWallet)
            }
            is LinkHandler.ProcessingState.CompletedWithPaymentResult -> {
                onPaymentResult(processingState.result)
            }
            is LinkHandler.ProcessingState.Error -> {
                onError(processingState.message?.resolvableString)
            }
            LinkHandler.ProcessingState.Launched -> {
                startProcessing(CheckoutIdentifier.SheetTopWallet)
            }
            is LinkHandler.ProcessingState.PaymentDetailsCollected -> {
                processingState.paymentSelection?.let {
                    // Link PaymentDetails was created successfully, use it to confirm the Stripe Intent.
                    updateSelection(it)
                    checkout(selection.value, CheckoutIdentifier.SheetBottomBuy)
                } ?: run {
                    // Link PaymentDetails creating failed, fallback to regular checkout.
                    // paymentSelection is already set to the card parameters from the form.
                    checkout(selection.value, CheckoutIdentifier.SheetBottomBuy)
                }
            }
            LinkHandler.ProcessingState.Ready -> {
                this.checkoutIdentifier = CheckoutIdentifier.SheetBottomBuy
                viewState.value = PaymentSheetViewState.Reset()
            }
            LinkHandler.ProcessingState.Started -> {
                this.checkoutIdentifier = CheckoutIdentifier.SheetBottomBuy
                viewState.value = PaymentSheetViewState.StartProcessing
            }
            LinkHandler.ProcessingState.CompleteWithoutLink -> {
                checkout()
            }
        }
    }

    private suspend fun loadPaymentSheetState() {
        val result = withContext(workContext) {
            paymentElementLoader.load(
                initializationMode = args.initializationMode,
                configuration = args.config.asCommonConfiguration(),
                isReloadingAfterProcessDeath = intentConfirmationHandler.hasReloadedFromProcessDeath,
                initializedViaCompose = args.initializedViaCompose,
            )
        }

        result.fold(
            onSuccess = { handlePaymentSheetStateLoaded(it) },
            onFailure = { handlePaymentSheetStateLoadFailure(it) },
        )
    }

    private fun handlePaymentSheetStateLoadFailure(error: Throwable) {
        setPaymentMethodMetadata(null)
        onFatal(error)
    }

    private suspend fun handlePaymentSheetStateLoaded(state: PaymentSheetState.Full) {
        val pendingResult = intentConfirmationHandler.awaitIntentResult()

        if (pendingResult is PaymentConfirmationResult.Succeeded) {
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
        customerStateHolder.setCustomerState(state.customer)

        updateSelection(state.paymentSelection)

        setPaymentMethodMetadata(state.paymentMethodMetadata)

        linkHandler.setupLink(state.linkState)

        val pendingFailedPaymentResult = intentConfirmationHandler.awaitIntentResult()
            as? PaymentConfirmationResult.Failed
        val errorMessage = pendingFailedPaymentResult?.cause?.stripeErrorMessage()

        resetViewState(errorMessage)
        navigationHandler.resetTo(
            determineInitialBackStack(
                paymentMethodMetadata = state.paymentMethodMetadata,
                customerStateHolder = customerStateHolder,
            )
        )

        viewModelScope.launch {
            intentConfirmationHandler.state.collectLatest { state ->
                when (state) {
                    is IntentConfirmationHandler.State.Idle -> Unit
                    is IntentConfirmationHandler.State.Preconfirming -> {
                        if (
                            state.inPreconfirmFlow &&
                            state.confirmationOption is PaymentConfirmationOption.GooglePay
                        ) {
                            setContentVisible(false)
                        } else {
                            setContentVisible(true)
                        }

                        startProcessing(checkoutIdentifier)
                    }
                    is IntentConfirmationHandler.State.Confirming -> {
                        setContentVisible(true)

                        if (viewState.value !is PaymentSheetViewState.StartProcessing) {
                            startProcessing(checkoutIdentifier)
                        }
                    }
                    is IntentConfirmationHandler.State.Complete -> {
                        setContentVisible(true)
                        processIntentResult(state.result)
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
        if (shouldLaunchCvcRecollectionScreen()) {
            launchCvcRecollection()
            return
        }
        checkout(selection.value, CheckoutIdentifier.SheetBottomBuy)
    }

    fun checkoutWithGooglePay() {
        checkout(PaymentSelection.GooglePay, CheckoutIdentifier.SheetTopWallet)
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

    override fun handleConfirmUSBankAccount(paymentSelection: PaymentSelection.New.USBankAccount) {
        updateSelection(paymentSelection)
        eventReporter.onPressConfirmButton(selection.value)
        checkout()
    }

    override fun clearErrorMessages() {
        if (viewState.value is PaymentSheetViewState.Reset) {
            viewState.value = PaymentSheetViewState.Reset(message = null)
        }
    }

    private fun launchCvcRecollection() {
        cvcRecollectionHandler.launch(
            paymentSelection = selection.value
        ) { cvcRecollectionData ->
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
        linkHandler.registerFromActivity(activityResultCaller)

        intentConfirmationHandler.register(activityResultCaller, lifecycleOwner)

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    linkHandler.unregisterFromActivity()
                    super.onDestroy(owner)
                }
            }
        )
    }

    @Suppress("ComplexCondition")
    private fun paymentSelectionWithCvcIfEnabled(paymentSelection: PaymentSelection?): PaymentSelection? {
        if (paymentSelection !is PaymentSelection.Saved) return paymentSelection
        return if (shouldAttachCvc()) {
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
            val confirmationOption = paymentSelectionWithCvcIfEnabled(paymentSelection)
                ?.toPaymentConfirmationOption(args.initializationMode, config.asCommonConfiguration())

            confirmationOption?.let { option ->
                val stripeIntent = awaitStripeIntent()

                intentConfirmationHandler.start(
                    arguments = IntentConfirmationHandler.Args(
                        intent = stripeIntent,
                        confirmationOption = option,
                    ),
                )
            } ?: run {
                val message = paymentSelection?.let {
                    "Cannot confirm using a ${it::class.qualifiedName} payment selection!"
                } ?: "Cannot confirm without a payment selection!"

                val exception = IllegalStateException(message)

                val event = paymentSelection?.let {
                    ErrorReporter.UnexpectedErrorEvent.PAYMENT_SHEET_INVALID_PAYMENT_SELECTION_ON_CHECKOUT
                } ?: ErrorReporter.UnexpectedErrorEvent.PAYMENT_SHEET_NO_PAYMENT_SELECTION_ON_CHECKOUT

                errorReporter.report(event, StripeException.create(exception))

                processIntentResult(
                    PaymentConfirmationResult.Failed(
                        cause = exception,
                        message = exception.stripeErrorMessage(),
                        type = PaymentConfirmationErrorType.Internal,
                    )
                )
            }
        }
    }

    override fun onPaymentResult(paymentResult: PaymentResult) {
        viewModelScope.launch(workContext) {
            val stripeIntent = awaitStripeIntent()
            processPayment(stripeIntent, paymentResult)
        }
    }

    private fun handlePaymentFailed(
        error: PaymentSheetConfirmationError,
        message: ResolvableString
    ) {
        eventReporter.onPaymentFailure(
            paymentSelection = selection.value,
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
        val currentSelection = selection.value
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

        if (finishImmediately) {
            _paymentSheetResult.tryEmit(PaymentSheetResult.Completed)
        } else {
            viewState.value = PaymentSheetViewState.FinishProcessing {
                _paymentSheetResult.tryEmit(PaymentSheetResult.Completed)
            }
        }
    }

    private fun processIntentResult(result: PaymentConfirmationResult?) {
        when (result) {
            is PaymentConfirmationResult.Succeeded -> handlePaymentCompleted(
                intent = result.intent,
                deferredIntentConfirmationType = result.deferredIntentConfirmationType,
                finishImmediately = false,
            )
            is PaymentConfirmationResult.Failed -> processIntentFailure(result)
            is PaymentConfirmationResult.Canceled,
            null -> resetViewState()
        }
    }

    private fun processIntentFailure(failure: PaymentConfirmationResult.Failed) {
        when (failure.type) {
            PaymentConfirmationErrorType.Payment -> handlePaymentFailed(
                error = PaymentSheetConfirmationError.Stripe(failure.cause),
                message = failure.message,
            )
            PaymentConfirmationErrorType.ExternalPaymentMethod -> handlePaymentFailed(
                error = PaymentSheetConfirmationError.ExternalPaymentMethod,
                message = failure.message,
            )
            is PaymentConfirmationErrorType.GooglePay -> handlePaymentFailed(
                error = PaymentSheetConfirmationError.GooglePay(failure.type.errorCode),
                message = failure.message,
            )
            PaymentConfirmationErrorType.Fatal -> onFatal(failure.cause)
            PaymentConfirmationErrorType.MerchantIntegration,
            PaymentConfirmationErrorType.Internal -> onError(failure.message)
        }
    }

    private fun processPayment(stripeIntent: StripeIntent, paymentResult: PaymentResult) {
        when (paymentResult) {
            is PaymentResult.Completed -> {
                handlePaymentCompleted(
                    intent = stripeIntent,
                    deferredIntentConfirmationType = null,
                    finishImmediately = false
                )
            }
            is PaymentResult.Failed -> {
                handlePaymentFailed(
                    error = PaymentSheetConfirmationError.Stripe(paymentResult.throwable),
                    message = paymentResult.throwable.stripeErrorMessage(),
                )
            }
            is PaymentResult.Canceled -> {
                resetViewState()
            }
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

            val component = DaggerPaymentSheetLauncherComponent
                .builder()
                .application(application)
                .build()
                .paymentSheetViewModelSubcomponentBuilder
                .paymentSheetViewModelModule(PaymentSheetViewModelModule(starterArgsSupplier()))
                .savedStateHandle(savedStateHandle)
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
}

private val PaymentSheet.InitializationMode.isProcessingPayment: Boolean
    get() = when (this) {
        is PaymentSheet.InitializationMode.PaymentIntent -> true
        is PaymentSheet.InitializationMode.SetupIntent -> false
        is PaymentSheet.InitializationMode.DeferredIntent -> {
            intentConfiguration.mode is PaymentSheet.IntentConfiguration.Mode.Payment
        }
    }
