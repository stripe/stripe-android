package com.stripe.android.paymentsheet

import android.app.Application
import android.os.Bundle
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntegerRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentLauncher.Companion.LINK_ENABLED
import com.stripe.android.link.injection.LinkPaymentLauncherFactory
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.DaggerPaymentSheetLauncherComponent
import com.stripe.android.paymentsheet.injection.PaymentSheetViewModelModule
import com.stripe.android.paymentsheet.injection.PaymentSheetViewModelSubcomponent
import com.stripe.android.paymentsheet.model.ConfirmStripeIntentParamsFactory
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.model.getSupportedSavedCustomerPMs
import com.stripe.android.paymentsheet.paymentdatacollection.ach.ACHText
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.repositories.initializeRepositoryAndGetStripeIntent
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

/**
 * This is used by both the [PaymentSheetActivity] and the [PaymentSheetAddPaymentMethodFragment]
 * classes to convert a [PaymentSheetViewState] to a [PrimaryButton.State]
 */
internal fun PaymentSheetViewState.convert(): PrimaryButton.State {
    return when (this) {
        is PaymentSheetViewState.Reset ->
            PrimaryButton.State.Ready
        is PaymentSheetViewState.StartProcessing ->
            PrimaryButton.State.StartProcessing
        is PaymentSheetViewState.FinishProcessing ->
            PrimaryButton.State.FinishProcessing(this.onComplete)
    }
}

internal class PaymentSheetViewModel @Inject internal constructor(
    // Properties provided through PaymentSheetViewModelComponent.Builder
    application: Application,
    internal val args: PaymentSheetContract.Args,
    eventReporter: EventReporter,
    // Properties provided through injection
    private val lazyPaymentConfig: Lazy<PaymentConfiguration>,
    private val stripeIntentRepository: StripeIntentRepository,
    private val stripeIntentValidator: StripeIntentValidator,
    customerRepository: CustomerRepository,
    prefsRepository: PrefsRepository,
    resourceRepository: ResourceRepository,
    private val paymentLauncherFactory: StripePaymentLauncherAssistedFactory,
    private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
    logger: Logger,
    @IOContext workContext: CoroutineContext,
    @InjectorKey injectorKey: String,
    savedStateHandle: SavedStateHandle,
    linkPaymentLauncherFactory: LinkPaymentLauncherFactory
) : BaseSheetViewModel<PaymentSheetViewModel.TransitionTarget>(
    application = application,
    config = args.config,
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    prefsRepository = prefsRepository,
    workContext = workContext,
    logger = logger,
    injectorKey = injectorKey,
    resourceRepository = resourceRepository,
    savedStateHandle = savedStateHandle,
    linkPaymentLauncherFactory = linkPaymentLauncherFactory
) {
    private val confirmParamsFactory = ConfirmStripeIntentParamsFactory.createFactory(
        args.clientSecret
    )

    @VisibleForTesting
    internal val _paymentSheetResult = MutableLiveData<PaymentSheetResult>()
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

    // Holds a reference to the last selected payment method while checking out with Google Pay.
    // If Google Pay is cancelled or fails, it will be set again as the selected payment method.
    internal var lastSelectedPaymentMethod: PaymentSelection? = null

    internal val isProcessingPaymentIntent
        get() = args.clientSecret is PaymentIntentClientSecret

    override var newPaymentSelection: PaymentSelection.New? = null

    @VisibleForTesting
    internal var googlePayPaymentMethodLauncher: GooglePayPaymentMethodLauncher? = null

    private var linkActivityResultLauncher:
        ActivityResultLauncher<LinkActivityContract.Args>? = null

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
            isGooglePayReady,
            fragmentConfigEvent
        ).forEach {
            addSource(it) {
                value = (
                    isLinkEnabled.value == true ||
                        isGooglePayReady.value == true
                    ) && fragmentConfigEvent.value?.peekContent() != null
            }
        }
    }

    internal var paymentLauncher: PaymentLauncher? = null

    init {
        eventReporter.onInit(config)
        if (googlePayLauncherConfig == null) {
            savedStateHandle[SAVE_GOOGLE_PAY_READY] = false
        }
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
                        savedStateHandle[SAVE_GOOGLE_PAY_READY] = isReady
                    },
                    activityResultLauncher = activityResultLauncher
                )
        }
    }

    /**
     * Fetch the [StripeIntent] for the client secret received in the initialization arguments, if
     * not fetched yet. If successful, continues through validation and fetching the saved payment
     * methods for the customer.
     */
    internal fun maybeFetchStripeIntent() = if (stripeIntent.value == null) {
        viewModelScope.launch {
            // The co-routine scope is needed to do work off the UI thread
            CoroutineScope(workContext).launch {
                runCatching {
                    val intent = initializeRepositoryAndGetStripeIntent(
                        resourceRepository,
                        stripeIntentRepository,
                        args.clientSecret,
                        eventReporter
                    )

                    lpmServerSpec =
                        resourceRepository.getLpmRepository().serverSpecLoadingState.serverLpmSpecs

                    // The lpm server specs need to be saved so that upon the
                    // activity being killed the state can be restored.
                    intent
                }.fold(
                    onSuccess = {
                        withContext(Dispatchers.Main) {
                            onStripeIntentFetchResponse(it)
                        }
                    },
                    onFailure = {
                        withContext(Dispatchers.Main) {
                            setStripeIntent(null)
                            onFatal(it)
                        }
                    }
                )
            }
        }
        true
    } else {
        false
    }

    private fun onStripeIntentFetchResponse(stripeIntent: StripeIntent) {
        runCatching {
            stripeIntentValidator.requireValid(stripeIntent)
        }.fold(
            onSuccess = {
                savedStateHandle[SAVE_STRIPE_INTENT] = stripeIntent
                updatePaymentMethods(stripeIntent)
                setupLink(stripeIntent)
                resetViewState()
            },
            onFailure = ::onFatal
        )
    }

    /**
     * Fetch the saved payment methods for the customer, if a [PaymentSheet.CustomerConfiguration]
     * was provided.
     * It will fetch only the payment method types as defined in [SupportedPaymentMethod.getSupportedSavedCustomerPMs].
     */
    @VisibleForTesting
    fun updatePaymentMethods(stripeIntent: StripeIntent) {
        viewModelScope.launch {
            runCatching {
                customerConfig?.let { customerConfig ->
                    getSupportedSavedCustomerPMs(
                        stripeIntent,
                        config,
                        resourceRepository.getLpmRepository()
                    ).mapNotNull {
                        // The SDK is only able to parse customer LPMs
                        // that are hard coded in the SDK.
                        PaymentMethod.Type.fromCode(it.code)
                    }.let {
                        customerRepository.getPaymentMethods(
                            customerConfig,
                            it
                        )
                    }.filter { paymentMethod ->
                        paymentMethod.hasExpectedDetails().also { valid ->
                            if (!valid) {
                                logger.error(
                                    "Discarding invalid payment method ${paymentMethod.id}"
                                )
                            }
                        }
                    }
                }.orEmpty()
            }.fold(
                onSuccess = {
                    savedStateHandle[SAVE_PAYMENT_METHODS] = it
                    setStripeIntent(stripeIntent)
                    resetViewState()
                },
                onFailure = ::onFatal
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

    fun checkout(checkoutIdentifier: CheckoutIdentifier) {
        startProcessing(checkoutIdentifier)

        val paymentSelection = selection.value

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

    /**
     * Used to set up any dependencies that require a reference to the current Activity.
     * Must be called from the Activity's `onCreate`.
     */
    fun registerFromActivity(activityResultCaller: ActivityResultCaller) {
        linkActivityResultLauncher = activityResultCaller.registerForActivityResult(
            LinkActivityContract(),
            ::onLinkActivityResult
        )

        paymentLauncher = paymentLauncherFactory.create(
            { lazyPaymentConfig.get().publishableKey },
            { lazyPaymentConfig.get().stripeAccountId },
            activityResultCaller.registerForActivityResult(
                PaymentLauncherContract(),
                ::onPaymentResult
            )
        )
    }

    /**
     * Used to clean up any dependencies that require a reference to the current Activity.
     * Must be called from the Activity's `onDestroy`.
     */
    fun unregisterFromActivity() {
        linkActivityResultLauncher = null
        paymentLauncher = null
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

    override fun setupLink(stripeIntent: StripeIntent) {
        if (LINK_ENABLED &&
            stripeIntent.paymentMethodTypes.contains(PaymentMethod.Type.Link.code)
        ) {
            viewModelScope.launch {
                when (
                    linkLauncher.setup(
                        stripeIntent,
                        (newPaymentSelection as? PaymentSelection.New.LinkInline)
                            ?.linkPaymentDetails,
                        this
                    )
                ) {
                    AccountStatus.Verified -> launchLink()
                    AccountStatus.VerificationStarted,
                    AccountStatus.NeedsVerification -> {
                        linkVerificationCallback = { success ->
                            linkVerificationCallback = null
                            _showLinkVerificationDialog.value = false

                            if (success) {
                                launchLink()
                            }
                        }
                        _showLinkVerificationDialog.value = true
                    }
                    AccountStatus.SignedOut -> {}
                }
                _isLinkEnabled.value = true
            }
        } else {
            _isLinkEnabled.value = false
        }
    }

    fun launchLink() {
        linkActivityResultLauncher?.let { activityResultLauncher ->
            linkLauncher.present(
                activityResultLauncher
            )
            onLinkLaunched()
        }
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
        val paymentResult = result.convertToPaymentResult()
        if (paymentResult is PaymentResult.Completed) {
            // If payment was completed, don't show the Payment Sheet.
            prefsRepository.savePaymentSelection(PaymentSelection.Link)
            _paymentSheetResult.value = PaymentSheetResult.Completed
        } else {
            setContentVisible(true)
            onPaymentResult(paymentResult)
        }
    }

    override fun onLinkPaymentDetailsCollected(linkPaymentDetails: LinkPaymentDetails?) {
        linkPaymentDetails?.let {
            // Link PaymentDetails was created successfully, use it to confirm the Stripe Intent.
            updateSelection(PaymentSelection.New.LinkInline(it))
            checkout(CheckoutIdentifier.SheetBottomBuy)
        } ?: run {
            // Link PaymentDetails creationg failed, fallback to regular checkout.
            // paymentSelection is already set to the card parameters from the form.
            checkout(CheckoutIdentifier.SheetBottomBuy)
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
                    is PaymentSelection.New -> stripeIntent.paymentMethod?.let {
                        PaymentSelection.Saved(it)
                    }
                    PaymentSelection.GooglePay -> selection.value
                    PaymentSelection.Link -> selection.value
                    is PaymentSelection.Saved -> selection.value
                    null -> null
                }?.let {
                    prefsRepository.savePaymentSelection(it)
                }

                _viewState.value = PaymentSheetViewState.FinishProcessing {
                    _paymentSheetResult.value = PaymentSheetResult.Completed
                }
            }
            else -> {
                eventReporter.onPaymentFailure(selection.value)

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
            is GooglePayPaymentMethodLauncher.Result.Completed ->
                confirmPaymentSelection(PaymentSelection.Saved(result.paymentMethod))
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
            is GooglePayPaymentMethodLauncher.Result.Canceled -> resetViewState()
        }
    }

    override fun onFatal(throwable: Throwable) {
        logger.error("Payment Sheet error", throwable)
        _fatal.value = throwable
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

    internal sealed class TransitionTarget {
        abstract val fragmentConfig: FragmentConfig

        // User has saved PM's and is selected
        data class SelectSavedPaymentMethod(
            override val fragmentConfig: FragmentConfig
        ) : TransitionTarget()

        // User has saved PM's and is adding a new one
        data class AddPaymentMethodFull(
            override val fragmentConfig: FragmentConfig
        ) : TransitionTarget()

        // User has no saved PM's
        data class AddPaymentMethodSheet(
            override val fragmentConfig: FragmentConfig
        ) : TransitionTarget()
    }

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> PaymentSheetContract.Args,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs),
        Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val application: Application
        )

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<PaymentSheetViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            savedStateHandle: SavedStateHandle
        ): T {
            val args = starterArgsSupplier()

            injectWithFallback(args.injectorKey, FallbackInitializeParam(applicationSupplier()))

            return subComponentBuilderProvider.get()
                .paymentSheetViewModelModule(PaymentSheetViewModelModule(args))
                .savedStateHandle(savedStateHandle)
                .build().viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam) {
            DaggerPaymentSheetLauncherComponent
                .builder()
                .application(arg.application)
                .injectorKey(DUMMY_INJECTOR_KEY)
                .build().inject(this)
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
