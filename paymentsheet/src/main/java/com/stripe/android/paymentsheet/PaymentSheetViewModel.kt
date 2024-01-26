package com.stripe.android.paymentsheet

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntegerRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.injection.DaggerPaymentSheetLauncherComponent
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.injection.PaymentSheetViewModelModule
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.state.asPaymentSheetLoadingException
import com.stripe.android.paymentsheet.ui.HeaderTextFactory
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.utils.canSave
import com.stripe.android.paymentsheet.utils.combineStateFlows
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.PrimaryButtonUiStateMapper
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.requireApplication
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import com.stripe.android.R as StripeR

internal class PaymentSheetViewModel @Inject internal constructor(
    // Properties provided through PaymentSheetViewModelComponent.Builder
    application: Application,
    internal val args: PaymentSheetContractV2.Args,
    eventReporter: EventReporter,
    // Properties provided through injection
    private val lazyPaymentConfig: Lazy<PaymentConfiguration>,
    private val paymentSheetLoader: PaymentSheetLoader,
    customerRepository: CustomerRepository,
    prefsRepository: PrefsRepository,
    lpmRepository: LpmRepository,
    private val paymentLauncherFactory: StripePaymentLauncherAssistedFactory,
    private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
    private val bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
    logger: Logger,
    @IOContext workContext: CoroutineContext,
    savedStateHandle: SavedStateHandle,
    linkHandler: LinkHandler,
    linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
    formViewModelSubComponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>,
    editInteractorFactory: ModifiableEditPaymentMethodViewInteractor.Factory
) : BaseSheetViewModel(
    application = application,
    config = args.config,
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    prefsRepository = prefsRepository,
    workContext = workContext,
    logger = logger,
    lpmRepository = lpmRepository,
    savedStateHandle = savedStateHandle,
    linkHandler = linkHandler,
    linkConfigurationCoordinator = linkConfigurationCoordinator,
    headerTextFactory = HeaderTextFactory(isCompleteFlow = true),
    formViewModelSubComponentBuilderProvider = formViewModelSubComponentBuilderProvider,
    editInteractorFactory = editInteractorFactory
) {

    private val primaryButtonUiStateMapper = PrimaryButtonUiStateMapper(
        context = getApplication(),
        config = config,
        isProcessingPayment = isProcessingPaymentIntent,
        currentScreenFlow = currentScreen,
        buttonsEnabledFlow = buttonsEnabled,
        amountFlow = amount,
        selectionFlow = selection,
        customPrimaryButtonUiStateFlow = customPrimaryButtonUiState,
        onClick = {
            reportConfirmButtonPressed()
            checkout()
        },
    )

    private val _paymentSheetResult = MutableSharedFlow<PaymentSheetResult>(replay = 1)
    internal val paymentSheetResult: SharedFlow<PaymentSheetResult> = _paymentSheetResult

    @VisibleForTesting
    internal val viewState = MutableStateFlow<PaymentSheetViewState?>(null)

    internal var checkoutIdentifier: CheckoutIdentifier = CheckoutIdentifier.SheetBottomBuy

    val googlePayButtonState: StateFlow<PaymentSheetViewState?> = viewState.filter {
        checkoutIdentifier == CheckoutIdentifier.SheetTopGooglePay
    }.stateIn(
        scope = CoroutineScope(workContext),
        started = SharingStarted.WhileSubscribed(),
        initialValue = null,
    )

    val buyButtonState: Flow<PaymentSheetViewState?> = viewState.filter {
        checkoutIdentifier == CheckoutIdentifier.SheetBottomBuy
    }

    internal val isProcessingPaymentIntent
        get() = args.initializationMode.isProcessingPayment

    override var newPaymentSelection: PaymentSelection.New? = null

    private var googlePayPaymentMethodLauncher: GooglePayPaymentMethodLauncher? = null

    private var bacsMandateConfirmationLauncher: BacsMandateConfirmationLauncher? = null

    private var deferredIntentConfirmationType: DeferredIntentConfirmationType? = null

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

    private var pendingPaymentResult: InternalPaymentResult? = null

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
                    merchantName = merchantName,
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

    override val error: StateFlow<String?> = buyButtonState.map { it?.errorMessage?.message }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    override val walletsState: StateFlow<WalletsState?> = combineStateFlows(
        linkHandler.isLinkEnabled,
        linkEmailFlow,
        googlePayState,
        googlePayButtonState,
        buttonsEnabled,
        supportedPaymentMethodsFlow,
        backStack,
    ) { isLinkAvailable, linkEmail, googlePayState, googlePayButtonState, buttonsEnabled, paymentMethodTypes, stack ->
        WalletsState.create(
            isLinkAvailable = isLinkAvailable,
            linkEmail = linkEmail,
            googlePayState = googlePayState,
            googlePayButtonState = googlePayButtonState,
            buttonsEnabled = buttonsEnabled,
            paymentMethodTypes = paymentMethodTypes,
            googlePayLauncherConfig = googlePayLauncherConfig,
            googlePayButtonType = googlePayButtonType,
            screen = stack.last(),
            isCompleteFlow = true,
            onGooglePayPressed = this::checkoutWithGooglePay,
            onLinkPressed = linkHandler::launchLink,
        )
    }

    private var paymentLauncher: StripePaymentLauncher? = null

    init {
        viewModelScope.launch {
            linkHandler.processingState.collect { processingState ->
                handleLinkProcessingState(processingState)
            }
        }

        AnalyticsRequestFactory.regenerateSessionId()

        val isDeferred = args.initializationMode is PaymentSheet.InitializationMode.DeferredIntent

        eventReporter.onInit(
            configuration = config,
            isDeferred = isDeferred,
        )

        viewModelScope.launch {
            loadPaymentSheetState()
        }
    }

    override val shouldCompleteLinkFlowInline: Boolean = true

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
                checkout()
            }
            is LinkHandler.ProcessingState.CompletedWithPaymentResult -> {
                onPaymentResult(processingState.result)
            }
            is LinkHandler.ProcessingState.Error -> {
                onError(processingState.message)
            }
            LinkHandler.ProcessingState.Launched -> {
                startProcessing(CheckoutIdentifier.SheetBottomBuy)
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
                updatePrimaryButtonState(PrimaryButton.State.Ready)
            }
            LinkHandler.ProcessingState.Started -> {
                updatePrimaryButtonState(PrimaryButton.State.StartProcessing)
            }
            LinkHandler.ProcessingState.CompleteWithoutLink -> {
                checkout()
            }
        }
    }

    private suspend fun loadPaymentSheetState() {
        val result = withContext(workContext) {
            paymentSheetLoader.load(args.initializationMode, args.config)
        }

        result.fold(
            onSuccess = ::handlePaymentSheetStateLoaded,
            onFailure = ::handlePaymentSheetStateLoadFailure,
        )
    }

    private fun handlePaymentSheetStateLoadFailure(error: Throwable) {
        val pendingResult = pendingPaymentResult

        if (pendingResult is InternalPaymentResult.Completed) {
            // If we just received a transaction result after process death, we don't error. Instead, we dismiss
            // PaymentSheet and return a `Completed` result to the caller.
            val usedPaymentMethod = error.asPaymentSheetLoadingException.usedPaymentMethod
            handlePaymentCompleted(usedPaymentMethod, finishImmediately = true)
        } else {
            setStripeIntent(null)
            onFatal(error)
        }

        pendingPaymentResult = null
    }

    private fun handlePaymentSheetStateLoaded(state: PaymentSheetState.Full) {
        cbcEligibility = when (state.isEligibleForCardBrandChoice) {
            true -> CardBrandChoiceEligibility.Eligible(
                preferredNetworks = state.config.preferredNetworks
            )
            false -> CardBrandChoiceEligibility.Ineligible
        }

        savedStateHandle[SAVE_PAYMENT_METHODS] = state.customerPaymentMethods
        updateSelection(state.paymentSelection)

        savedStateHandle[SAVE_GOOGLE_PAY_STATE] = if (state.isGooglePayReady) {
            GooglePayState.Available
        } else {
            GooglePayState.NotAvailable
        }

        setStripeIntent(state.stripeIntent)

        val linkState = state.linkState

        linkHandler.setupLink(linkState)

        val pendingFailedPaymentResult = pendingPaymentResult as? InternalPaymentResult.Failed
        val errorMessage = pendingFailedPaymentResult?.throwable?.stripeErrorMessage(getApplication())

        resetViewState(errorMessage)
        transitionToFirstScreen()
    }

    fun setupGooglePay(
        lifecycleScope: CoroutineScope,
        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>
    ) {
        googlePayLauncherConfig?.let { config ->
            googlePayPaymentMethodLauncher =
                googlePayPaymentMethodLauncherFactory.create(
                    lifecycleScope = lifecycleScope,
                    config = config,
                    readyCallback = { /* Nothing to do here */ },
                    activityResultLauncher = activityResultLauncher
                )
        }
    }

    private fun resetViewState(userErrorMessage: String? = null) {
        viewState.value =
            PaymentSheetViewState.Reset(userErrorMessage?.let { UserErrorMessage(it) })
        savedStateHandle[SAVE_PROCESSING] = false
    }

    private fun startProcessing(checkoutIdentifier: CheckoutIdentifier) {
        if (this.checkoutIdentifier != checkoutIdentifier) {
            // Clear out any previous errors before setting the new button to get updates.
            viewState.value = PaymentSheetViewState.Reset()
        }

        this.checkoutIdentifier = checkoutIdentifier
        savedStateHandle[SAVE_PROCESSING] = true
        viewState.value = PaymentSheetViewState.StartProcessing
    }

    fun checkout() {
        val paymentSelection = selection.value
        checkout(paymentSelection, CheckoutIdentifier.SheetBottomBuy)
    }

    fun checkoutWithGooglePay() {
        setContentVisible(false)
        checkout(PaymentSelection.GooglePay, CheckoutIdentifier.SheetTopGooglePay)
    }

    private fun checkout(
        paymentSelection: PaymentSelection?,
        identifier: CheckoutIdentifier,
    ) {
        startProcessing(identifier)

        if (paymentSelection is PaymentSelection.GooglePay) {
            stripeIntent.value?.let { stripeIntent ->
                googlePayPaymentMethodLauncher?.present(
                    currencyCode = (stripeIntent as? PaymentIntent)?.currency
                        ?: args.googlePayConfig?.currencyCode.orEmpty(),
                    amount = when (stripeIntent) {
                        is PaymentIntent -> stripeIntent.amount ?: 0L
                        is SetupIntent -> args.googlePayConfig?.amount ?: 0L
                    },
                    transactionId = stripeIntent.id,
                    label = args.googlePayConfig?.label,
                )
            }
        } else if (
            paymentSelection is PaymentSelection.New.GenericPaymentMethod &&
            paymentSelection.paymentMethodCreateParams.typeCode == PaymentMethod.Type.BacsDebit.code
        ) {
            BacsMandateData.fromPaymentSelection(paymentSelection)?.let { data ->
                runCatching {
                    requireNotNull(bacsMandateConfirmationLauncher)
                }.onSuccess { launcher ->
                    launcher.launch(
                        data = data,
                        appearance = config.appearance
                    )
                }.onFailure {
                    resetViewState(
                        userErrorMessage = getApplication<Application>()
                            .resources
                            .getString(R.string.stripe_something_went_wrong)
                    )
                }
            } ?: run {
                resetViewState(
                    userErrorMessage = getApplication<Application>()
                        .resources
                        .getString(R.string.stripe_something_went_wrong)
                )
            }
        } else {
            confirmPaymentSelection(paymentSelection)
        }
    }

    fun confirmStripeIntent(confirmStripeIntentParams: ConfirmStripeIntentParams) {
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
            onFailure = ::onFatal
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
            onFailure = ::onFatal
        )
    }

    override fun handlePaymentMethodSelected(selection: PaymentSelection?) {
        if (!editing.value && selection != this.selection.value) {
            updateSelection(selection)
        }
    }

    override fun handleConfirmUSBankAccount(paymentSelection: PaymentSelection.New.USBankAccount) {
        updateSelection(paymentSelection)
        reportConfirmButtonPressed()
        checkout()
    }

    override fun clearErrorMessages() {
        if (viewState.value is PaymentSheetViewState.Reset) {
            viewState.value = PaymentSheetViewState.Reset(message = null)
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

        val bacsActivityResultLauncher = activityResultCaller.registerForActivityResult(
            BacsMandateConfirmationContract(),
            ::onBacsMandateResult
        )

        bacsMandateConfirmationLauncher = bacsMandateConfirmationLauncherFactory.create(
            bacsActivityResultLauncher
        )

        paymentLauncher = paymentLauncherFactory.create(
            publishableKey = { lazyPaymentConfig.get().publishableKey },
            stripeAccountId = { lazyPaymentConfig.get().stripeAccountId },
            statusBarColor = args.statusBarColor,
            hostActivityLauncher = activityResultCaller.registerForActivityResult(
                PaymentLauncherContract(),
                ::onInternalPaymentResult
            ),
            includePaymentSheetAuthenticators = true,
        )

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    paymentLauncher = null
                    bacsMandateConfirmationLauncher = null
                    bacsActivityResultLauncher.unregister()
                    linkHandler.unregisterFromActivity()
                    super.onDestroy(owner)
                }
            }
        )
    }

    private fun confirmPaymentSelection(paymentSelection: PaymentSelection?) {
        viewModelScope.launch {
            val stripeIntent = requireNotNull(stripeIntent.value)

            val nextStep = intentConfirmationInterceptor.intercept(
                initializationMode = args.initializationMode,
                paymentSelection = paymentSelection,
                shippingValues = args.config.shippingDetails?.toConfirmPaymentIntentShipping(),
            )

            deferredIntentConfirmationType = nextStep.deferredIntentConfirmationType

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
                    onError(nextStep.message)
                }
                is IntentConfirmationInterceptor.NextStep.Complete -> {
                    processPayment(stripeIntent, PaymentResult.Completed)
                }
            }
        }
    }

    override fun onPaymentResult(paymentResult: PaymentResult) {
        viewModelScope.launch {
            runCatching {
                requireNotNull(stripeIntent.value)
            }.fold(
                onSuccess = { stripeIntent ->
                    processPayment(stripeIntent, paymentResult)
                },
                onFailure = ::onFatal
            )
        }
    }

    private fun onInternalPaymentResult(launcherResult: InternalPaymentResult) {
        val intent = stripeIntent.value

        if (intent == null) {
            // We're recovering from process death. Wait for the pending payment result
            // to be handled after re-loading.
            pendingPaymentResult = launcherResult
            return
        }

        when (launcherResult) {
            is InternalPaymentResult.Completed -> {
                processPayment(launcherResult.intent, PaymentResult.Completed)
            }
            is InternalPaymentResult.Failed -> {
                processPayment(intent, PaymentResult.Failed(launcherResult.throwable))
            }
            is InternalPaymentResult.Canceled -> {
                processPayment(intent, PaymentResult.Canceled)
            }
        }
    }

    private fun handlePaymentFailed(error: Throwable) {
        eventReporter.onPaymentFailure(
            paymentSelection = selection.value,
            error = PaymentSheetConfirmationError.Stripe(error),
        )

        resetViewState(
            userErrorMessage = error.stripeErrorMessage(getApplication())
        )
    }

    private fun handlePaymentCompleted(paymentMethod: PaymentMethod?, finishImmediately: Boolean) {
        eventReporter.onPaymentSuccess(
            paymentSelection = selection.value,
            deferredIntentConfirmationType = deferredIntentConfirmationType,
        )

        // Reset after sending event
        deferredIntentConfirmationType = null

        /*
         * Sets current selection as default payment method in future payment sheet usage. New payment
         * methods are only saved if the payment sheet is in setup mode, is in payment intent with setup
         * for usage, or the customer has requested the payment method be saved.
         */
        when (val currentSelection = selection.value) {
            is PaymentSelection.New -> paymentMethod.takeIf {
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

    private fun processPayment(stripeIntent: StripeIntent, paymentResult: PaymentResult) {
        when (paymentResult) {
            is PaymentResult.Completed -> {
                handlePaymentCompleted(paymentMethod = stripeIntent.paymentMethod, finishImmediately = false)
            }
            is PaymentResult.Failed -> {
                handlePaymentFailed(paymentResult.throwable)
            }
            is PaymentResult.Canceled -> {
                resetViewState()
            }
        }
    }

    internal fun onGooglePayResult(result: GooglePayPaymentMethodLauncher.Result) {
        setContentVisible(true)
        when (result) {
            is GooglePayPaymentMethodLauncher.Result.Completed -> {
                val newPaymentSelection = PaymentSelection.Saved(
                    paymentMethod = result.paymentMethod,
                    walletType = PaymentSelection.Saved.WalletType.GooglePay,
                )

                updateSelection(newPaymentSelection)
                confirmPaymentSelection(newPaymentSelection)
            }
            is GooglePayPaymentMethodLauncher.Result.Failed -> {
                logger.error("Error processing Google Pay payment", result.error)
                eventReporter.onPaymentFailure(
                    paymentSelection = PaymentSelection.GooglePay,
                    error = PaymentSheetConfirmationError.GooglePay(result.errorCode),
                )
                onError(
                    when (result.errorCode) {
                        GooglePayPaymentMethodLauncher.NETWORK_ERROR ->
                            StripeR.string.stripe_failure_connection_error
                        else -> StripeR.string.stripe_internal_error
                    }
                )
            }
            is GooglePayPaymentMethodLauncher.Result.Canceled -> {
                resetViewState()
            }
        }
    }

    private fun onBacsMandateResult(result: BacsMandateConfirmationResult) {
        when (result) {
            is BacsMandateConfirmationResult.Confirmed -> {
                val paymentSelection = selection.value

                if (
                    paymentSelection is PaymentSelection.New.GenericPaymentMethod &&
                    paymentSelection.paymentMethodCreateParams.typeCode == PaymentMethod.Type.BacsDebit.code
                ) {
                    confirmPaymentSelection(paymentSelection)
                }
            }
            is BacsMandateConfirmationResult.ModifyDetails,
            is BacsMandateConfirmationResult.Cancelled -> resetViewState(userErrorMessage = null)
        }
    }

    override fun onFatal(throwable: Throwable) {
        logger.error("Payment Sheet error", throwable)
        mostRecentError = throwable
        _paymentSheetResult.tryEmit(PaymentSheetResult.Failed(throwable))
    }

    override fun onUserCancel() {
        reportDismiss()
        _paymentSheetResult.tryEmit(PaymentSheetResult.Canceled)
    }

    override fun onFinish() {
        _paymentSheetResult.tryEmit(PaymentSheetResult.Completed)
    }

    override fun onError(@IntegerRes error: Int?) =
        onError(error?.let { getApplication<Application>().resources.getString(it) })

    override fun onError(error: String?) = resetViewState(error)

    override fun determineInitialBackStack(): List<PaymentSheetScreen> {
        val hasPaymentMethods = !paymentMethods.value.isNullOrEmpty()
        val target = if (hasPaymentMethods) {
            PaymentSheetScreen.SelectSavedPaymentMethods
        } else {
            PaymentSheetScreen.AddFirstPaymentMethod
        }
        return listOf(target)
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
        SheetTopGooglePay,
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
