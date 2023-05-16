package com.stripe.android.paymentsheet

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntegerRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.CreateIntentCallbackForServerSideConfirmation
import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.IntentConfirmationInterceptor
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod.Type.Card
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.extensions.registerPollingAuthenticator
import com.stripe.android.paymentsheet.extensions.unregisterPollingAuthenticator
import com.stripe.android.paymentsheet.injection.DaggerPaymentSheetLauncherComponent
import com.stripe.android.paymentsheet.injection.PaymentSheetViewModelModule
import com.stripe.android.paymentsheet.injection.PaymentSheetViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.state.WalletsContainerState
import com.stripe.android.paymentsheet.ui.HeaderTextFactory
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.PrimaryButtonUiStateMapper
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.requireApplication
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import com.stripe.android.R as StripeR

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
internal class PaymentSheetViewModel @Inject internal constructor(
    // Properties provided through PaymentSheetViewModelComponent.Builder
    application: Application,
    internal val args: PaymentSheetContractV2.Args,
    eventReporter: EventReporter,
    // Properties provided through injection
    private val lazyPaymentConfig: Lazy<PaymentConfiguration>,
    private val stripeIntentValidator: StripeIntentValidator,
    private val paymentSheetLoader: PaymentSheetLoader,
    customerRepository: CustomerRepository,
    prefsRepository: PrefsRepository,
    lpmRepository: LpmRepository,
    private val paymentLauncherFactory: StripePaymentLauncherAssistedFactory,
    private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
    logger: Logger,
    @IOContext workContext: CoroutineContext,
    savedStateHandle: SavedStateHandle,
    linkHandler: LinkHandler,
    private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
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
    headerTextFactory = HeaderTextFactory(isCompleteFlow = true),
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
        onClick = this::checkout,
    )

    private val _paymentSheetResult = MutableSharedFlow<PaymentSheetResult>(replay = 1)
    internal val paymentSheetResult: SharedFlow<PaymentSheetResult> = _paymentSheetResult

    @VisibleForTesting
    internal val viewState = MutableStateFlow<PaymentSheetViewState?>(null)

    internal var checkoutIdentifier: CheckoutIdentifier = CheckoutIdentifier.SheetBottomBuy

    val googlePayButtonState: Flow<PaymentSheetViewState?> = viewState.filter {
        checkoutIdentifier == CheckoutIdentifier.SheetTopGooglePay
    }

    val buyButtonState: Flow<PaymentSheetViewState?> = viewState.filter {
        checkoutIdentifier == CheckoutIdentifier.SheetBottomBuy
    }

    internal val isProcessingPaymentIntent
        get() = args.initializationMode.isProcessingPayment

    private val isDecoupling: Boolean
        get() = args.initializationMode is PaymentSheet.InitializationMode.DeferredIntent

    override var newPaymentSelection: PaymentSelection.New? = null

    private var googlePayPaymentMethodLauncher: GooglePayPaymentMethodLauncher? = null

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
                    merchantName = merchantName
                )
            }
        }

    override val primaryButtonUiState = primaryButtonUiStateMapper.forCompleteFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null,
    )

    internal val walletsContainerState: Flow<WalletsContainerState> = combine(
        linkHandler.isLinkEnabled,
        googlePayState,
        supportedPaymentMethodsFlow,
    ) { isLinkAvailable, googlePayState, paymentMethodTypes ->
        WalletsContainerState(
            showLink = isLinkAvailable == true,
            showGooglePay = googlePayState.isReadyForUse,
            dividerTextResource = if (paymentMethodTypes.singleOrNull() == Card.code) {
                R.string.stripe_paymentsheet_or_pay_with_card
            } else {
                R.string.stripe_paymentsheet_or_pay_using
            },
            googlePayAllowCreditCards = googlePayLauncherConfig?.allowCreditCards ?: false,
            googlePayBillingAddressParameters = googlePayLauncherConfig?.let {
                GooglePayJsonFactory.BillingAddressParameters(
                    it.billingAddressConfig.isRequired,
                    when (it.billingAddressConfig.format) {
                        GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Min ->
                            GooglePayJsonFactory.BillingAddressParameters.Format.Min
                        GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full ->
                            GooglePayJsonFactory.BillingAddressParameters.Format.Full
                    },
                    it.billingAddressConfig.isPhoneNumberRequired
                )
            }
        )
    }

    private var paymentLauncher: StripePaymentLauncher? = null

    init {
        viewModelScope.launch {
            linkHandler.processingState.collect { processingState ->
                handleLinkProcessingState(processingState)
            }
        }

        val isServerSideConfirmation = isDecoupling &&
            IntentConfirmationInterceptor.createIntentCallback is CreateIntentCallbackForServerSideConfirmation

        eventReporter.onInit(
            configuration = config,
            isDecoupling = isDecoupling,
            isServerSideConfirmation = isServerSideConfirmation,
        )

        viewModelScope.launch {
            loadPaymentSheetState()
        }
    }

    override val shouldCompleteLinkFlowInline: Boolean = true

    fun handleLinkPressed() {
        linkHandler.launchLink()
    }

    private fun handleLinkProcessingState(processingState: LinkHandler.ProcessingState) {
        when (processingState) {
            LinkHandler.ProcessingState.Cancelled -> {
                _paymentSheetResult.tryEmit(PaymentSheetResult.Canceled)
            }
            LinkHandler.ProcessingState.Completed -> {
                eventReporter.onPaymentSuccess(
                    paymentSelection = PaymentSelection.Link,
                    currency = stripeIntent.value?.currency,
                    isDecoupling = isDecoupling,
                )
                prefsRepository.savePaymentSelection(PaymentSelection.Link)
                _paymentSheetResult.tryEmit(PaymentSheetResult.Completed)
            }
            is LinkHandler.ProcessingState.CompletedWithPaymentResult -> {
                setContentVisible(true)
                onPaymentResult(processingState.result)
            }
            is LinkHandler.ProcessingState.Error -> {
                onError(processingState.message)
            }
            LinkHandler.ProcessingState.Launched -> {
                setContentVisible(false)
                startProcessing(CheckoutIdentifier.SheetBottomBuy)
            }
            is LinkHandler.ProcessingState.PaymentDetailsCollected -> {
                processingState.details?.let {
                    // Link PaymentDetails was created successfully, use it to confirm the Stripe Intent.
                    updateSelection(PaymentSelection.New.LinkInline(it))
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
        }
    }

    private suspend fun loadPaymentSheetState() {
        val result = withContext(workContext) {
            paymentSheetLoader.load(args.initializationMode, args.config)
        }

        when (result) {
            is PaymentSheetLoader.Result.Success -> {
                handlePaymentSheetStateLoaded(result.state)
            }
            is PaymentSheetLoader.Result.Failure -> {
                setStripeIntent(null)
                onFatal(result.throwable)
            }
        }
    }

    private fun handlePaymentSheetStateLoaded(state: PaymentSheetState.Full) {
        lpmServerSpec = lpmRepository.serverSpecLoadingState.serverLpmSpecs

        savedStateHandle[SAVE_PAYMENT_METHODS] = state.customerPaymentMethods
        updateSelection(state.paymentSelection)

        savedStateHandle[SAVE_GOOGLE_PAY_STATE] = if (state.isGooglePayReady) {
            GooglePayState.Available
        } else {
            GooglePayState.NotAvailable
        }

        setStripeIntent(state.stripeIntent)

        val linkState = state.linkState

        linkHandler.setupLinkLaunchingEagerly(viewModelScope, linkState)

        resetViewState()
        transitionToFirstScreen()
    }

    fun setupGooglePay(
        lifecycleScope: CoroutineScope,
        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>
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
                    amount = (stripeIntent as? PaymentIntent)?.amount?.toInt() ?: 0,
                    transactionId = stripeIntent.id
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
    fun registerFromActivity(activityResultCaller: ActivityResultCaller) {
        linkHandler.registerFromActivity(activityResultCaller)

        paymentLauncher = paymentLauncherFactory.create(
            { lazyPaymentConfig.get().publishableKey },
            { lazyPaymentConfig.get().stripeAccountId },
            activityResultCaller.registerForActivityResult(
                PaymentLauncherContract(),
                ::onPaymentResult
            )
        ).also {
            it.registerPollingAuthenticator()
        }
    }

    /**
     * Used to clean up any dependencies that require a reference to the current Activity.
     * Must be called from the Activity's `onDestroy`.
     */
    fun unregisterFromActivity() {
        paymentLauncher?.unregisterPollingAuthenticator()
        paymentLauncher = null
        linkHandler.unregisterFromActivity()
    }

    private fun confirmPaymentSelection(paymentSelection: PaymentSelection?) {
        viewModelScope.launch {
            val stripeIntent = requireNotNull(stripeIntent.value)

            val nextStep = intentConfirmationInterceptor.intercept(
                clientSecret = stripeIntent.clientSecret,
                paymentSelection = paymentSelection,
                shippingValues = args.config?.shippingDetails?.toConfirmPaymentIntentShipping(),
            )

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
                    if (nextStep.isForceSuccess) {
                        eventReporter.onForceSuccess()
                    }
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

    private fun processPayment(stripeIntent: StripeIntent, paymentResult: PaymentResult) {
        when (paymentResult) {
            is PaymentResult.Completed -> {
                eventReporter.onPaymentSuccess(
                    paymentSelection = selection.value,
                    currency = stripeIntent.currency,
                    isDecoupling = isDecoupling,
                )

                // Default future payments to the selected payment method. New payment methods won't
                // be the default because we don't know if the user selected save for future use.
                when (selection.value) {
                    is PaymentSelection.New -> null
                    else -> selection.value
                }?.let {
                    prefsRepository.savePaymentSelection(it)
                }

                viewState.value = PaymentSheetViewState.FinishProcessing {
                    _paymentSheetResult.tryEmit(PaymentSheetResult.Completed)
                }
            }
            else -> {
                if (paymentResult is PaymentResult.Failed) {
                    eventReporter.onPaymentFailure(
                        paymentSelection = selection.value,
                        currency = stripeIntent.currency,
                        isDecoupling = isDecoupling,
                    )
                }

                runCatching {
                    stripeIntentValidator.requireValid(stripeIntent)
                }.fold(
                    onSuccess = {
                        resetViewState(
                            when (paymentResult) {
                                is PaymentResult.Failed -> paymentResult.throwable.localizedMessage
                                else -> null // indicates canceled payment
                            }
                        )
                    },
                    onFailure = ::onFatal
                )
            }
        }
    }

    internal fun onGooglePayResult(result: GooglePayPaymentMethodLauncher.Result) {
        setContentVisible(true)
        when (result) {
            is GooglePayPaymentMethodLauncher.Result.Completed -> {
                val newPaymentSelection = PaymentSelection.Saved(
                    paymentMethod = result.paymentMethod,
                    isGooglePay = true,
                )

                updateSelection(newPaymentSelection)
                confirmPaymentSelection(newPaymentSelection)
            }
            is GooglePayPaymentMethodLauncher.Result.Failed -> {
                logger.error("Error processing Google Pay payment", result.error)
                eventReporter.onPaymentFailure(
                    paymentSelection = PaymentSelection.GooglePay,
                    currency = stripeIntent.value?.currency,
                    isDecoupling = isDecoupling,
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

    override fun onFatal(throwable: Throwable) {
        logger.error("Payment Sheet error", throwable)
        mostRecentError = throwable
        _paymentSheetResult.tryEmit(PaymentSheetResult.Failed(throwable))
    }

    override fun onUserCancel() {
        _paymentSheetResult.tryEmit(PaymentSheetResult.Canceled)
    }

    override fun onFinish() {
        _paymentSheetResult.tryEmit(PaymentSheetResult.Completed)
    }

    override fun onError(@IntegerRes error: Int?) =
        onError(error?.let { getApplication<Application>().resources.getString(it) })

    override fun onError(error: String?) = resetViewState(error)

    override fun transitionToFirstScreen() {
        val target = if (paymentMethods.value.isNullOrEmpty()) {
            PaymentSheetScreen.AddFirstPaymentMethod
        } else {
            PaymentSheetScreen.SelectSavedPaymentMethods
        }
        transitionTo(target)
    }

    internal class Factory(
        private val starterArgsSupplier: () -> PaymentSheetContractV2.Args,
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {

        internal data class FallbackInitializeParam(val application: Application)

        @Inject
        lateinit var subComponentBuilderProvider: Provider<PaymentSheetViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = starterArgsSupplier()

            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            val injector = injectWithFallback(
                injectorKey = args.injectorKey,
                fallbackInitializeParam = FallbackInitializeParam(application),
            )

            val subcomponent = subComponentBuilderProvider.get()
                .paymentSheetViewModelModule(PaymentSheetViewModelModule(args))
                .savedStateHandle(savedStateHandle)
                .build()
            val viewModel = subcomponent.viewModel
            viewModel.injector = requireNotNull(injector as NonFallbackInjector)
            return viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam): Injector {
            val component = DaggerPaymentSheetLauncherComponent
                .builder()
                .application(arg.application)
                .injectorKey(DUMMY_INJECTOR_KEY)
                .build()
            component.inject(this)
            return component
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

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
private val PaymentSheet.InitializationMode.isProcessingPayment: Boolean
    get() = when (this) {
        is PaymentSheet.InitializationMode.PaymentIntent -> true
        is PaymentSheet.InitializationMode.SetupIntent -> false
        is PaymentSheet.InitializationMode.DeferredIntent -> {
            intentConfiguration.mode is PaymentSheet.IntentConfiguration.Mode.Payment
        }
    }
