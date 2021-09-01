package com.stripe.android.paymentsheet

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntegerRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.StripeIntentResult
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.DaggerPaymentSheetViewModelComponent
import com.stripe.android.paymentsheet.model.ConfirmStripeIntentParamsFactory
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.view.AuthActivityStarterHost
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
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

@Singleton
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
    private val paymentFlowResultProcessorProvider:
        Provider<PaymentFlowResultProcessor<out StripeIntent, StripeIntentResult<StripeIntent>>>,
    prefsRepository: PrefsRepository,
    private val paymentController: PaymentController,
    private val googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
    private val logger: Logger,
    @IOContext workContext: CoroutineContext
) : BaseSheetViewModel<PaymentSheetViewModel.TransitionTarget>(
    application = application,
    config = args.config,
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    prefsRepository = prefsRepository,
    workContext = workContext
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
        outputLiveData.addSource(_viewState) { currentValue ->
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

    override var newCard: PaymentSelection.New.Card? = null

    @VisibleForTesting
    internal var googlePayPaymentMethodLauncher: GooglePayPaymentMethodLauncher? = null

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

    init {
        eventReporter.onInit(config)
        if (googlePayLauncherConfig == null) {
            _isGooglePayReady.value = false
        }
    }

    private fun apiThrowableToString(throwable: Throwable): String? {
        return when (throwable) {
            is APIConnectionException -> {
                getApplication<Application>().resources.getString(
                    R.string.stripe_failure_connection_error
                )
            }
            else -> {
                throwable.localizedMessage
            }
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
                        _isGooglePayReady.value = isReady
                    },
                    activityResultLauncher = activityResultLauncher
                )
        }
    }

    /**
     * Fetch the [StripeIntent] for the client secret received as parameter. If successful,
     * continues through validation and fetching the saved payment methods for the customer.
     */
    fun fetchStripeIntent() {
        PaymentSheetActivity.transitionFragmentResource.increment()
        viewModelScope.launch {
            runCatching {
                stripeIntentRepository.get(args.clientSecret)
            }.fold(
                onSuccess = ::onStripeIntentFetchResponse,
                onFailure = {
                    setStripeIntent(null)
                    onFatal(it)
                }
            )
        }
    }

    private fun onStripeIntentFetchResponse(stripeIntent: StripeIntent) {
        runCatching {
            stripeIntentValidator.requireValid(stripeIntent)
        }.fold(
            onSuccess = {
                updatePaymentMethods(stripeIntent)
                resetViewState()
            },
            onFailure = ::onFatal
        )
    }

    /**
     * Fetch the saved payment methods for the customer, if a [PaymentSheet.CustomerConfiguration]
     * was provided.
     * It will fetch only the payment method types accepted by the [stripeIntent] and defined in
     * [SupportedPaymentMethod.supportedSavedPaymentMethods].
     */
    @VisibleForTesting
    fun updatePaymentMethods(stripeIntent: StripeIntent) {
        viewModelScope.launch {
            runCatching {
                customerConfig?.let { customerConfig ->
                    stripeIntent.paymentMethodTypes.mapNotNull {
                        PaymentMethod.Type.fromCode(it)
                    }.filter {
                        SupportedPaymentMethod.supportedSavedPaymentMethods.contains(it.code)
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
                    _paymentMethods.value = it
                    setStripeIntent(stripeIntent)
                    PaymentSheetActivity.transitionFragmentResource.decrement()
                    resetViewState()
                },
                onFailure = ::onFatal
            )
        }
    }

    private fun resetViewState(@IntegerRes stringResId: Int?) {
        resetViewState(
            stringResId?.let { getApplication<Application>().resources.getString(it) }
        )
    }

    private fun resetViewState(userErrorMessage: String? = null) {
        _viewState.value =
            PaymentSheetViewState.Reset(userErrorMessage?.let { UserErrorMessage(it) })
        _processing.value = false
    }

    fun checkout(checkoutIdentifier: CheckoutIdentifier) {
        if (this.checkoutIdentifier != checkoutIdentifier) {
            // Clear out any previous errors before setting the new button to get updates.
            _viewState.value = PaymentSheetViewState.Reset()
        }

        this.checkoutIdentifier = checkoutIdentifier
        _processing.value = true
        _viewState.value = PaymentSheetViewState.StartProcessing

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

    suspend fun confirmStripeIntent(
        authActivityStarterHost: AuthActivityStarterHost,
        confirmStripeIntentParams: ConfirmStripeIntentParams
    ) {
        paymentController.startConfirmAndAuth(
            authActivityStarterHost,
            confirmStripeIntentParams,
            ApiRequest.Options(
                lazyPaymentConfig.get().publishableKey,
                lazyPaymentConfig.get().stripeAccountId
            )
        )
    }

    fun registerFromActivity(activityResultCaller: ActivityResultCaller) {
        paymentController.registerLaunchersWithActivityResultCaller(
            activityResultCaller,
            ::onPaymentFlowResult
        )
    }

    fun unregisterFromActivity() {
        paymentController.unregisterLaunchers()
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

    private fun onStripeIntentResult(
        stripeIntentResult: StripeIntentResult<StripeIntent>
    ) {
        when (stripeIntentResult.outcome) {
            StripeIntentResult.Outcome.SUCCEEDED -> {
                eventReporter.onPaymentSuccess(selection.value)

                // SavedSelection needs to happen after new cards have been saved.
                when (selection.value) {
                    is PaymentSelection.New -> stripeIntentResult.intent.paymentMethod?.let {
                        PaymentSelection.Saved(it)
                    }
                    PaymentSelection.GooglePay -> selection.value
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
                    stripeIntentValidator.requireValid(stripeIntentResult.intent)
                }.fold(
                    onSuccess = {
                        resetViewState(
                            stripeIntentResult.failureMessage
                        )
                    },
                    onFailure = ::onFatal
                )
            }
        }
    }

    internal fun onGooglePayResult(result: GooglePayPaymentMethodLauncher.Result) {
        when (result) {
            is GooglePayPaymentMethodLauncher.Result.Completed ->
                confirmPaymentSelection(PaymentSelection.Saved(result.paymentMethod))
            is GooglePayPaymentMethodLauncher.Result.Failed -> {
                logger.error("Error processing Google Pay payment", result.error)
                eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                resetViewState(
                    when (result.errorCode) {
                        GooglePayPaymentMethodLauncher.NETWORK_ERROR ->
                            R.string.stripe_failure_connection_error
                        else -> R.string.stripe_google_pay_error_internal
                    }
                )
            }
            is GooglePayPaymentMethodLauncher.Result.Canceled -> resetViewState()
        }
    }

    fun onPaymentFlowResult(paymentFlowResult: PaymentFlowResult.Unvalidated) {
        viewModelScope.launch {
            runCatching {
                withContext(workContext) {
                    paymentFlowResultProcessorProvider.get().processResult(
                        paymentFlowResult
                    )
                }
            }.fold(
                onSuccess = {
                    onStripeIntentResult(it)
                },
                onFailure = { error ->
                    selection.value?.let {
                        eventReporter.onPaymentFailure(it)
                    }

                    resetViewState(apiThrowableToString(error))
                }
            )
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

    internal sealed class TransitionTarget {
        abstract val fragmentConfig: com.stripe.android.paymentsheet.model.FragmentConfig

        // User has saved PM's and is selected
        data class SelectSavedPaymentMethod(
            override val fragmentConfig: com.stripe.android.paymentsheet.model.FragmentConfig
        ) : TransitionTarget()

        // User has saved PM's and is adding a new one
        data class AddPaymentMethodFull(
            override val fragmentConfig: com.stripe.android.paymentsheet.model.FragmentConfig
        ) : TransitionTarget()

        // User has no saved PM's
        data class AddPaymentMethodSheet(
            override val fragmentConfig: com.stripe.android.paymentsheet.model.FragmentConfig
        ) : TransitionTarget()
    }

    @Suppress("UNCHECKED_CAST")
    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> PaymentSheetContract.Args
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return DaggerPaymentSheetViewModelComponent.builder()
                .application(applicationSupplier())
                .starterArgs(starterArgsSupplier())
                .build()
                .viewModel as T
        }
    }

    /**
     * This is the identifier of the caller of the [checkout] function.  It is used in
     * the observables of [viewState] to get state events related to it.
     */
    internal enum class CheckoutIdentifier {
        AddFragmentTopGooglePay,
        SheetBottomGooglePay,
        SheetBottomBuy,
        None
    }
}
