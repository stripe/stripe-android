package com.stripe.android.paymentsheet

import android.app.Application
import androidx.activity.result.ActivityResultCaller
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
import com.stripe.android.PaymentController
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.googlepaylauncher.GooglePayConfig
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayLauncherResult
import com.stripe.android.googlepaylauncher.StripeGooglePayContract
import com.stripe.android.googlepaylauncher.getErrorResourceID
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.DaggerPaymentSheetViewModelComponent
import com.stripe.android.paymentsheet.injection.IOContext
import com.stripe.android.paymentsheet.model.ConfirmStripeIntentParamsFactory
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.repositories.PaymentMethodsRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * This is used by both the [PaymentSheetActivity] and the [PaymentSheetAddCardFragment] classes
 * to convert a [PaymentSheetViewState] to a [PrimaryButton.State]
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
    private val eventReporter: EventReporter,
    // Properties provided through injection
    private val apiRequestOptions: ApiRequest.Options,
    private val stripeIntentRepository: StripeIntentRepository,
    private val paymentMethodsRepository: PaymentMethodsRepository,
    private val paymentFlowResultProcessor: dagger.Lazy<PaymentFlowResultProcessor<out StripeIntent, StripeIntentResult<StripeIntent>>>,
    private val googlePayRepository: GooglePayRepository,
    prefsRepository: PrefsRepository,
    private val logger: Logger,
    @IOContext workContext: CoroutineContext,
    private val paymentController: PaymentController
) : BaseSheetViewModel<PaymentSheetViewModel.TransitionTarget>(
    application = application,
    config = args.config,
    prefsRepository = prefsRepository,
    workContext = workContext
) {
    private val confirmParamsFactory =
        ConfirmStripeIntentParamsFactory.createFactory(args.clientSecret)

    @VisibleForTesting
    internal val _paymentSheetResult = MutableLiveData<PaymentSheetResult>()
    internal val paymentSheetResult: LiveData<PaymentSheetResult> = _paymentSheetResult

    private val _startConfirm = MutableLiveData<Event<ConfirmStripeIntentParams>>()
    internal val startConfirm: LiveData<Event<ConfirmStripeIntentParams>> = _startConfirm

    @VisibleForTesting
    internal val _amount = MutableLiveData<Amount>()
    internal val amount: LiveData<Amount> = _amount

    @VisibleForTesting
    internal val _viewState = MutableLiveData<PaymentSheetViewState>(null)
    internal val viewState: LiveData<PaymentSheetViewState> = _viewState.distinctUntilChanged()

    internal var checkoutIdentifier: CheckoutIdentifier = CheckoutIdentifier.SheetBottomBuy
    internal fun getButtonStateObservable(checkoutIdentifier: CheckoutIdentifier): MediatorLiveData<PaymentSheetViewState?> {
        val outputLiveData = MediatorLiveData<PaymentSheetViewState?>()
        outputLiveData.addSource(_viewState) { currentValue ->
            if (this.checkoutIdentifier == checkoutIdentifier) {
                outputLiveData.value = currentValue
            }
        }
        return outputLiveData
    }

    internal val isProcessingPaymentIntent
        get() = args.clientSecret is PaymentIntentClientSecret

    override var newCard: PaymentSelection.New.Card? = null

    private val stripeIntentValidator = StripeIntentValidator()

    init {
        fetchIsGooglePayReady()
        eventReporter.onInit(config)
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

    fun fetchIsGooglePayReady() {
        if (isGooglePayReady.value == null) {
            if (args.isGooglePayEnabled) {
                viewModelScope.launch {
                    val isGooglePayReady = withContext(workContext) {
                        googlePayRepository.isReady().first()
                    }
                    _isGooglePayReady.value = isGooglePayReady
                }
            } else {
                _isGooglePayReady.value = false
            }
        }
    }

    fun updatePaymentMethods() {
        viewModelScope.launch {
            _paymentMethods.value = customerConfig?.let {
                paymentMethodsRepository.get(
                    it,
                    PaymentMethod.Type.Card
                )
            }.orEmpty()
        }
    }

    fun fetchStripeIntent() {
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
        if (stripeIntent.isConfirmed) {
            onConfirmedStripeIntent(stripeIntent)
        } else {
            runCatching {
                stripeIntentValidator.requireValid(stripeIntent)
            }.fold(
                onSuccess = {
                    setStripeIntent(stripeIntent)
                    resetViewState(stripeIntent, userErrorMessage = null)
                },
                onFailure = ::onFatal
            )
        }
    }

    /**
     * There's nothing left to be done in payment sheet if the [StripeIntent] is confirmed.
     *
     * See [How intents work](https://stripe.com/docs/payments/intents) for more details.
     */
    private fun onConfirmedStripeIntent(stripeIntent: StripeIntent) {
        logger.info(
            """
            ${stripeIntent.javaClass.simpleName} with id=${stripeIntent.id} has already been confirmed.
            """.trimIndent()
        )
        _viewState.value = PaymentSheetViewState.FinishProcessing {
            _paymentSheetResult.value = PaymentSheetResult.Completed
        }
    }

    private fun resetViewState(stripeIntent: StripeIntent, @IntegerRes stringResId: Int?) {
        resetViewState(
            stripeIntent,
            stringResId?.let { getApplication<Application>().resources.getString(it) }
        )
    }

    private fun resetViewState(stripeIntent: StripeIntent, userErrorMessage: String?) {
        when (stripeIntent) {
            is PaymentIntent -> {
                val amount = stripeIntent.amount
                val currencyCode = stripeIntent.currency
                if (amount != null && currencyCode != null) {
                    _amount.value = Amount(amount, currencyCode)
                    _viewState.value =
                        PaymentSheetViewState.Reset(userErrorMessage?.let { UserErrorMessage(it) })
                } else {
                    onFatal(
                        IllegalStateException("PaymentIntent could not be parsed correctly.")
                    )
                }
            }
            is SetupIntent ->
                _viewState.value =
                    PaymentSheetViewState.Reset(userErrorMessage?.let { UserErrorMessage(it) })
        }

        _processing.value = false
    }

    fun checkout(checkoutIdentifier: CheckoutIdentifier) {
        // Clear out any previous errors before setting the new button to get updates.
        _viewState.value = PaymentSheetViewState.Reset(null)

        this.checkoutIdentifier = checkoutIdentifier
        _processing.value = true
        _viewState.value = PaymentSheetViewState.StartProcessing

        val paymentSelection = selection.value

        if (paymentSelection is PaymentSelection.GooglePay) {
            if (stripeIntent.value !is PaymentIntent) {
                logger.error(
                    "Expected PaymentIntent when checking out with Google Pay," +
                        " but found '${stripeIntent.value}'"
                )
            }
            (stripeIntent.value as? PaymentIntent)?.let { paymentIntent ->
                _launchGooglePay.value = Event(
                    StripeGooglePayContract.Args(
                        config = GooglePayConfig(
                            environment = when (args.config?.googlePay?.environment) {
                                PaymentSheet.GooglePayConfiguration.Environment.Production ->
                                    GooglePayEnvironment.Production
                                else ->
                                    GooglePayEnvironment.Test
                            },
                            amount = paymentIntent.amount?.toInt(),
                            countryCode = args.googlePayConfig?.countryCode.orEmpty(),
                            currencyCode = paymentIntent.currency.orEmpty(),
                            merchantName = args.config?.merchantDisplayName,
                            transactionId = paymentIntent.id
                        ),
                        statusBarColor = args.statusBarColor
                    )
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
            apiRequestOptions
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
            is PaymentSelection.New.Card -> {
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
                    is PaymentSelection.New.Card -> stripeIntentResult.intent.paymentMethod?.let {
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
                            it,
                            stripeIntentResult.failureMessage
                        )
                    },
                    onFailure = ::onFatal
                )
            }
        }
    }

    internal fun onGooglePayResult(
        googlePayResult: GooglePayLauncherResult
    ) {
        when (googlePayResult) {
            is GooglePayLauncherResult.PaymentData -> {
                val paymentSelection = PaymentSelection.Saved(
                    googlePayResult.paymentMethod
                )
                confirmPaymentSelection(paymentSelection)
            }
            is GooglePayLauncherResult.Error -> {
                logger.error("Error processing Google Pay payment", googlePayResult.exception)
                eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                stripeIntent.value?.let { it ->
                    resetViewState(
                        it,
                        googlePayResult.getErrorResourceID()
                    )
                }
            }
            else -> {
                stripeIntent.value?.let { resetViewState(it, userErrorMessage = null) }
            }
        }
    }

    fun onPaymentFlowResult(paymentFlowResult: PaymentFlowResult.Unvalidated) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(workContext) {
                    paymentFlowResultProcessor.get().processResult(
                        paymentFlowResult
                    )
                }
            }

            result.fold(
                onSuccess = {
                    onStripeIntentResult(it)
                },
                onFailure = { error ->
                    selection.value?.let {
                        eventReporter.onPaymentFailure(it)
                    }

                    stripeIntent.value?.let { resetViewState(it, apiThrowableToString(error)) }
                }
            )
        }
    }

    override fun onFatal(throwable: Throwable) {
        _fatal.value = throwable
        _paymentSheetResult.value = PaymentSheetResult.Failed(throwable)
    }

    override fun onUserCancel() {
        _paymentSheetResult.value = PaymentSheetResult.Canceled
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
        private val eventReporterSupplier: (() -> EventReporter)? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return DaggerPaymentSheetViewModelComponent.builder()
                .application(applicationSupplier())
                .starterArgs(starterArgsSupplier())
                .eventReporter(
                    eventReporterSupplier?.let {
                        it()
                    } ?: run {
                        DefaultEventReporter(
                            mode = EventReporter.Mode.Complete,
                            starterArgsSupplier().sessionId,
                            applicationSupplier()
                        )
                    }
                )
                .build()
                .viewModel as T
        }
    }

    /**
     * This class represents the long value amount to charge and the currency code of the amount.
     */
    data class Amount(val value: Long, val currencyCode: String)

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
