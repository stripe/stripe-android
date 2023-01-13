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
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkActivityResult.Canceled.Reason
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
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
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import com.stripe.android.utils.requireApplication
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
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
    linkLauncher: LinkPaymentLauncher
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
    linkLauncher = linkLauncher
) {
    private val confirmParamsFactory = ConfirmStripeIntentParamsFactory.createFactory(
        args.clientSecret,
        args.config?.shippingDetails?.toConfirmPaymentIntentShipping()
    )

    private val _paymentSheetResult = MutableLiveData<PaymentSheetResult>()
    internal val paymentSheetResult: LiveData<PaymentSheetResult> = _paymentSheetResult

    private val _startConfirm = MutableLiveData<Event<ConfirmStripeIntentParams>>()
    internal val startConfirm: LiveData<Event<ConfirmStripeIntentParams>> = _startConfirm

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

    private var launchedLinkDirectly: Boolean = false

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
            isLinkEnabled,
            googlePayState.asLiveData(),
            isReadyEvents
        ).forEach {
            addSource(it) {
                value = (
                    isLinkEnabled.value == true ||
                        googlePayState.value == GooglePayState.Available
                    ) && isReadyEvents.value?.peekContent() == true
            }
        }
    }

    private var paymentLauncher: StripePaymentLauncher? = null

    init {
        eventReporter.onInit(config)
        if (googlePayLauncherConfig == null) {
            savedStateHandle[SAVE_GOOGLE_PAY_STATE] = GooglePayState.NotAvailable
        }

        viewModelScope.launch {
            loadPaymentSheetState()
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

        _isLinkEnabled.value = linkState != null
        activeLinkSession.value = linkState?.loginState == LinkState.LoginState.LoggedIn

        if (linkState != null) {
            setupLink(linkState)
        }

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
        linkLauncher.register(
            activityResultCaller,
            ::onLinkActivityResult,
        )

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
        linkLauncher.unregister()
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
            _startConfirm.value = Event(confirmParams)
        }
    }

    private fun setupLink(state: LinkState) {
        _linkConfiguration.value = state.configuration

        when (state.loginState) {
            LinkState.LoginState.LoggedIn -> {
                launchLink(state.configuration, launchedDirectly = true)
            }
            LinkState.LoginState.NeedsVerification -> {
                setupLinkWithVerification(state.configuration)
            }
            LinkState.LoginState.LoggedOut -> {
                // Nothing to do here
            }
        }
    }

    private fun setupLinkWithVerification(
        configuration: LinkPaymentLauncher.Configuration,
    ) {
        viewModelScope.launch {
            val success = requestLinkVerification()
            if (success) {
                launchLink(configuration, launchedDirectly = true)
            }
        }
    }

    override fun completeLinkInlinePayment(
        configuration: LinkPaymentLauncher.Configuration,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        isReturningUser: Boolean
    ) {
        if (isReturningUser) {
            launchLink(configuration, launchedDirectly = false, paymentMethodCreateParams)
        } else {
            super.completeLinkInlinePayment(
                configuration,
                paymentMethodCreateParams,
                isReturningUser
            )
        }
    }

    fun launchLink(
        configuration: LinkPaymentLauncher.Configuration,
        launchedDirectly: Boolean,
        paymentMethodCreateParams: PaymentMethodCreateParams? = null
    ) {
        launchedLinkDirectly = launchedDirectly

        linkLauncher.present(
            configuration,
            paymentMethodCreateParams,
        )

        onLinkLaunched()
    }

    /**
     * Method called when the Link UI is launched. Should be used to update the PaymentSheet UI
     * accordingly.
     */
    private fun onLinkLaunched() {
        setContentVisible(false)
        startProcessing(CheckoutIdentifier.SheetBottomBuy)
    }

    /**
     * Method called with the result of launching the Link UI to collect a payment.
     */
    private fun onLinkActivityResult(result: LinkActivityResult) {
        val completePaymentFlow = result is LinkActivityResult.Completed
        val cancelPaymentFlow = launchedLinkDirectly &&
            result is LinkActivityResult.Canceled && result.reason == Reason.BackPressed

        if (completePaymentFlow) {
            // If payment was completed inside the Link UI, dismiss immediately.
            eventReporter.onPaymentSuccess(PaymentSelection.Link)
            prefsRepository.savePaymentSelection(PaymentSelection.Link)
            _paymentSheetResult.value = PaymentSheetResult.Completed
        } else if (cancelPaymentFlow) {
            // We launched the user straight into Link, but they decided to exit out of it.
            _paymentSheetResult.value = PaymentSheetResult.Canceled
        } else {
            setContentVisible(true)
            onPaymentResult(result.convertToPaymentResult())
        }
    }

    override fun onLinkPaymentDetailsCollected(linkPaymentDetails: LinkPaymentDetails.New?) {
        linkPaymentDetails?.let {
            // Link PaymentDetails was created successfully, use it to confirm the Stripe Intent.
            updateSelection(PaymentSelection.New.LinkInline(it))
            checkout()
        } ?: run {
            // Link PaymentDetails creation failed, fallback to regular checkout.
            // paymentSelection is already set to the card parameters from the form.
            checkout()
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
                    _paymentSheetResult.value = PaymentSheetResult.Completed
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
        _paymentSheetResult.value = PaymentSheetResult.Failed(throwable)
    }

    override fun onUserCancel() {
        _paymentSheetResult.value = PaymentSheetResult.Canceled
    }

    override fun onFinish() {
        _paymentSheetResult.value = PaymentSheetResult.Completed
    }

    override fun onError(@IntegerRes error: Int?) =
        onError(error?.let { getApplication<Application>().resources.getString(it) })

    override fun onError(error: String?) = resetViewState(error)

    private fun LinkActivityResult.convertToPaymentResult() =
        when (this) {
            is LinkActivityResult.Completed -> PaymentResult.Completed
            is LinkActivityResult.Canceled -> PaymentResult.Canceled
            is LinkActivityResult.Failed -> PaymentResult.Failed(error)
        }

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
