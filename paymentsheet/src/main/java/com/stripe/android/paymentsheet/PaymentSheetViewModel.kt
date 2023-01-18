package com.stripe.android.paymentsheet

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntegerRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
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
import com.stripe.android.model.PaymentMethod
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
import com.stripe.android.paymentsheet.model.ConfirmStripeIntentParamsFactory
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.paymentdatacollection.ach.ACHText
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.ui.HeaderTextFactory
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import com.stripe.android.utils.requireApplication
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetViewModel @Inject internal constructor(
    // Properties provided through PaymentSheetViewModelComponent.Builder
    application: Application,
    internal val args: PaymentSheetContract.Args,
    eventReporter: EventReporter,
    // Properties provided through injection
    private val lazyPaymentConfig: Lazy<PaymentConfiguration>,
    private val stripeIntentRepository: StripeIntentRepository,
    private val stripeIntentValidator: StripeIntentValidator,
    private val paymentSheetLoader: PaymentSheetLoader,
    customerRepository: CustomerRepository,
    prefsRepository: PrefsRepository,
    lpmResourceRepository: ResourceRepository<LpmRepository>,
    addressResourceRepository: ResourceRepository<AddressRepository>,
    private val paymentLauncherFactory: StripePaymentLauncherAssistedFactory,
    private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
    logger: Logger,
    @IOContext workContext: CoroutineContext,
    @InjectorKey injectorKey: String,
    savedStateHandle: SavedStateHandle,
    linkHandler: LinkHandler,
) : BaseSheetViewModel(
    application = application,
    config = args.config,
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    prefsRepository = prefsRepository,
    workContext = workContext,
    logger = logger,
    injectorKey = injectorKey,
    lpmResourceRepository = lpmResourceRepository,
    addressResourceRepository = addressResourceRepository,
    savedStateHandle = savedStateHandle,
    linkHandler = linkHandler,
    headerTextFactory = HeaderTextFactory(isCompleteFlow = true),
) {
    private val confirmParamsFactory = ConfirmStripeIntentParamsFactory.createFactory(
        args.clientSecret,
        args.config?.shippingDetails?.toConfirmPaymentIntentShipping()
    )

    private val _paymentSheetResult = MutableSharedFlow<PaymentSheetResult>(replay = 1)
    internal val paymentSheetResult: SharedFlow<PaymentSheetResult> = _paymentSheetResult

    @VisibleForTesting
    internal val _viewState = MutableLiveData<PaymentSheetViewState>(null)
    internal val viewState: LiveData<PaymentSheetViewState> = _viewState.distinctUntilChanged()

    internal var checkoutIdentifier: CheckoutIdentifier = CheckoutIdentifier.SheetBottomBuy
    internal fun getButtonStateObservable(
        checkoutIdentifier: CheckoutIdentifier
    ): MediatorLiveData<PaymentSheetViewState?> {
        val outputLiveData = MediatorLiveData<PaymentSheetViewState?>()
        outputLiveData.addSource(viewState) { currentValue ->
            if (this.checkoutIdentifier == checkoutIdentifier) {
                outputLiveData.value = currentValue
            }
        }
        return outputLiveData
    }

    internal val isProcessingPaymentIntent
        get() = args.clientSecret is PaymentIntentClientSecret

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

    // Whether the top container, containing Google Pay and Link buttons, should be visible
    internal val showTopContainer = MediatorLiveData<Boolean>().apply {
        listOf(
            linkHandler.isLinkEnabled.asLiveData(),
            googlePayState.asLiveData(),
            isReadyEvents
        ).forEach {
            addSource(it) {
                value = (
                    linkHandler.isLinkEnabled.value == true ||
                        googlePayState.value == GooglePayState.Available
                    ) && isReadyEvents.value?.peekContent() == true
            }
        }
    }

    private var paymentLauncher: StripePaymentLauncher? = null

    init {
        viewModelScope.launch {
            linkHandler.processingState.collect { processingState ->
                handleLinkProcessingState(processingState)
            }
        }

        eventReporter.onInit(config)
        if (googlePayLauncherConfig == null) {
            savedStateHandle[SAVE_GOOGLE_PAY_STATE] = GooglePayState.NotAvailable
        }

        viewModelScope.launch {
            loadPaymentSheetState()
        }
    }

    private fun handleLinkProcessingState(processingState: LinkHandler.ProcessingState) {
        when (processingState) {
            LinkHandler.ProcessingState.Cancelled -> {
                _paymentSheetResult.tryEmit(PaymentSheetResult.Canceled)
            }
            LinkHandler.ProcessingState.Completed -> {
                eventReporter.onPaymentSuccess(PaymentSelection.Link)
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
            paymentSheetLoader.load(args.clientSecret, args.config)
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
        lpmServerSpec = lpmResourceRepository.getRepository().serverSpecLoadingState.serverLpmSpecs

        savedStateHandle[SAVE_PAYMENT_METHODS] = state.customerPaymentMethods
        setStripeIntent(state.stripeIntent)

        val linkState = state.linkState

        linkHandler.setupLinkLaunchingEagerly(viewModelScope, linkState)

        resetViewState()
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
                    readyCallback = { isReady ->
                        savedStateHandle[SAVE_GOOGLE_PAY_STATE] = if (isReady) {
                            GooglePayState.Available
                        } else {
                            GooglePayState.NotAvailable
                        }
                    },
                    activityResultLauncher = activityResultLauncher
                )
        }
    }

    private fun resetViewState(userErrorMessage: String? = null) {
        _viewState.value =
            PaymentSheetViewState.Reset(userErrorMessage?.let { UserErrorMessage(it) })
        savedStateHandle[SAVE_PROCESSING] = false
    }

    private fun startProcessing(checkoutIdentifier: CheckoutIdentifier) {
        if (this.checkoutIdentifier != checkoutIdentifier) {
            // Clear out any previous errors before setting the new button to get updates.
            _viewState.value = PaymentSheetViewState.Reset()
        }

        this.checkoutIdentifier = checkoutIdentifier
        savedStateHandle[SAVE_PROCESSING] = true
        _viewState.value = PaymentSheetViewState.StartProcessing
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

    override fun updateSelection(selection: PaymentSelection?) {
        super.updateSelection(selection)

        when (selection) {
            is PaymentSelection.Saved -> {
                if (selection.paymentMethod.type == PaymentMethod.Type.USBankAccount) {
                    updateBelowButtonText(
                        ACHText.getContinueMandateText(getApplication())
                    )
                }
            }
            else -> {
                // no-op
            }
        }
    }

    override fun clearErrorMessages() {
        if (_viewState.value is PaymentSheetViewState.Reset) {
            _viewState.value = PaymentSheetViewState.Reset(message = null)
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
        when (paymentSelection) {
            is PaymentSelection.Saved -> {
                confirmParamsFactory.create(paymentSelection)
            }
            is PaymentSelection.New -> {
                confirmParamsFactory.create(paymentSelection)
            }
            else -> null
        }?.let { confirmParams ->
            confirmStripeIntent(confirmParams)
        }
    }

    override fun onPaymentResult(paymentResult: PaymentResult) {
        viewModelScope.launch {
            runCatching {
                stripeIntentRepository.get(args.clientSecret)
            }.fold(
                onSuccess = {
                    processPayment(it.intent, paymentResult)
                },
                onFailure = ::onFatal
            )
        }
    }

    private fun processPayment(stripeIntent: StripeIntent, paymentResult: PaymentResult) {
        when (paymentResult) {
            is PaymentResult.Completed -> {
                eventReporter.onPaymentSuccess(selection.value)

                // SavedSelection needs to happen after new cards have been saved.
                when (selection.value) {
                    is PaymentSelection.New.LinkInline -> PaymentSelection.Link
                    is PaymentSelection.New -> stripeIntent.paymentMethod?.let {
                        PaymentSelection.Saved(it)
                    }
                    else -> selection.value
                }?.let {
                    prefsRepository.savePaymentSelection(it)
                }

                _viewState.value = PaymentSheetViewState.FinishProcessing {
                    _paymentSheetResult.tryEmit(PaymentSheetResult.Completed)
                }
            }
            else -> {
                if (paymentResult is PaymentResult.Failed) {
                    eventReporter.onPaymentFailure(selection.value)
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
                confirmPaymentSelection(PaymentSelection.Saved(result.paymentMethod))
            }
            is GooglePayPaymentMethodLauncher.Result.Failed -> {
                logger.error("Error processing Google Pay payment", result.error)
                eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                onError(
                    when (result.errorCode) {
                        GooglePayPaymentMethodLauncher.NETWORK_ERROR ->
                            R.string.stripe_failure_connection_error
                        else -> R.string.stripe_internal_error
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

    fun transitionToFirstScreenWhenReady() {
        viewModelScope.launch {
            awaitReady()
            transitionToFirstScreen()
        }
    }

    private suspend fun awaitReady() {
        isReadyEvents.asFlow().filter { it.peekContent() }.first()
    }

    override fun transitionToFirstScreen() {
        val target = if (paymentMethods.value.isNullOrEmpty()) {
            updateSelection(null)
            PaymentSheetScreen.AddFirstPaymentMethod
        } else {
            PaymentSheetScreen.SelectSavedPaymentMethods
        }
        transitionTo(target)
    }

    internal class Factory(
        private val starterArgsSupplier: () -> PaymentSheetContract.Args,
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
