package com.stripe.android.paymentsheet

import android.app.Application
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
import com.stripe.android.PaymentIntentResult
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.googlepay.StripeGooglePayEnvironment
import com.stripe.android.googlepay.getErrorResourceID
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.payments.DefaultPaymentFlowResultProcessor
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentIntentValidator
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.repositories.PaymentMethodsApiRepository
import com.stripe.android.paymentsheet.repositories.PaymentMethodsRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

internal class PaymentSheetViewModel internal constructor(
    private val stripeIntentRepository: StripeIntentRepository,
    private val paymentMethodsRepository: PaymentMethodsRepository,
    private val paymentFlowResultProcessor: PaymentFlowResultProcessor,
    private val googlePayRepository: GooglePayRepository,
    prefsRepository: PrefsRepository,
    private val eventReporter: EventReporter,
    internal val args: PaymentSheetContract.Args,
    private val logger: Logger = Logger.noop(),
    workContext: CoroutineContext,
    application: Application
) : BaseSheetViewModel<PaymentSheetViewModel.TransitionTarget>(
    application = application,
    config = args.config,
    prefsRepository = prefsRepository,
    workContext = workContext
) {
    private val confirmParamsFactory = args.clientSecret.createConfirmParamsFactory()

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

    override var newCard: PaymentSelection.New.Card? = null

    private val paymentIntentValidator = PaymentIntentValidator()

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
                onSuccess = ::onPaymentIntentResponse,
                onFailure = {
                    _paymentIntent.value = null
                    onFatal(it)
                }
            )
        }
    }

    private fun onPaymentIntentResponse(paymentIntent: PaymentIntent) {
        if (paymentIntent.isConfirmed) {
            onConfirmedPaymentIntent(paymentIntent)
        } else {
            runCatching {
                paymentIntentValidator.requireValid(paymentIntent)
            }.fold(
                onSuccess = {
                    _paymentIntent.value = paymentIntent
                    resetViewState(paymentIntent, userErrorMessage = null)
                },
                onFailure = ::onFatal
            )
        }
    }

    /**
     * There's nothing left to be done in payment sheet if the PaymentIntent is confirmed.
     *
     * See [How intents work](https://stripe.com/docs/payments/intents) for more details.
     */
    private fun onConfirmedPaymentIntent(paymentIntent: PaymentIntent) {
        logger.info(
            """
            PaymentIntent with id=${paymentIntent.id}" has already been confirmed.
            """.trimIndent()
        )
        _viewState.value = PaymentSheetViewState.FinishProcessing {
            processResult(
                PaymentIntentResult(
                    paymentIntent,
                    StripeIntentResult.Outcome.SUCCEEDED
                )
            )
        }
    }

    private fun resetViewState(paymentIntent: PaymentIntent, @IntegerRes stringResId: Int?) {
        resetViewState(
            paymentIntent,
            stringResId?.let { getApplication<Application>().resources.getString(it) }
        )
    }

    private fun resetViewState(paymentIntent: PaymentIntent, userErrorMessage: String?) {
        val amount = paymentIntent.amount
        val currencyCode = paymentIntent.currency
        _processing.value = false
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

    fun checkout(checkoutIdentifier: CheckoutIdentifier) {
        // Clear out any previous errors before setting the new button to get updates.
        _viewState.value = PaymentSheetViewState.Reset(null)

        this.checkoutIdentifier = checkoutIdentifier
        _processing.value = true
        _viewState.value = PaymentSheetViewState.StartProcessing

        val paymentSelection = selection.value

        if (paymentSelection is PaymentSelection.GooglePay) {
            paymentIntent.value?.let { paymentIntent ->
                _launchGooglePay.value = Event(
                    StripeGooglePayContract.Args(
                        paymentIntent = paymentIntent,
                        config = StripeGooglePayContract.GooglePayConfig(
                            environment = when (args.config?.googlePay?.environment) {
                                PaymentSheet.GooglePayConfiguration.Environment.Production ->
                                    StripeGooglePayEnvironment.Production
                                else ->
                                    StripeGooglePayEnvironment.Test
                            },
                            countryCode = args.googlePayConfig?.countryCode.orEmpty(),
                            merchantName = args.config?.merchantDisplayName
                        ),
                        statusBarColor = args.statusBarColor
                    )
                )
            }
        } else {
            confirmPaymentSelection(paymentSelection)
        }
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

    private fun onPaymentIntentResult(paymentIntentResult: PaymentIntentResult) {
        when (paymentIntentResult.outcome) {
            StripeIntentResult.Outcome.SUCCEEDED -> {
                eventReporter.onPaymentSuccess(selection.value)

                // SavedSelection needs to happen after new cards have been saved.
                when (selection.value) {
                    is PaymentSelection.New.Card -> paymentIntentResult.intent.paymentMethod?.let {
                        PaymentSelection.Saved(it)
                    }
                    PaymentSelection.GooglePay -> selection.value
                    is PaymentSelection.Saved -> selection.value
                    null -> null
                }?.let {
                    prefsRepository.savePaymentSelection(it)
                }

                _viewState.value = PaymentSheetViewState.FinishProcessing {
                    processResult(paymentIntentResult)
                }
            }
            else -> {
                eventReporter.onPaymentFailure(selection.value)

                runCatching {
                    paymentIntentValidator.requireValid(paymentIntentResult.intent)
                }.fold(
                    onSuccess = {
                        resetViewState(
                            it,
                            paymentIntentResult.failureMessage
                        )
                    },
                    onFailure = ::onFatal
                )
            }
        }
    }

    private fun processResult(stripeIntentResult: PaymentIntentResult) {
        when (stripeIntentResult.outcome) {
            StripeIntentResult.Outcome.SUCCEEDED -> {
                _paymentSheetResult.value = PaymentSheetResult.Completed
            }
            else -> {
                // TODO(mshafrir-stripe): handle other outcomes
            }
        }
    }

    internal fun onGooglePayResult(
        googlePayResult: StripeGooglePayContract.Result
    ) {
        when (googlePayResult) {
            is StripeGooglePayContract.Result.PaymentData -> {
                val paymentSelection = PaymentSelection.Saved(
                    googlePayResult.paymentMethod
                )
                confirmPaymentSelection(paymentSelection)
            }
            is StripeGooglePayContract.Result.Error -> {
                logger.error("Error processing Google Pay payment", googlePayResult.exception)
                eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                paymentIntent.value?.let { it ->
                    resetViewState(
                        it,
                        googlePayResult.getErrorResourceID()
                    )
                }
            }
            else -> {
                paymentIntent.value?.let { resetViewState(it, userErrorMessage = null) }
            }
        }
    }

    fun onPaymentFlowResult(paymentFlowResult: PaymentFlowResult.Unvalidated) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(workContext) {
                    paymentFlowResultProcessor.processPaymentIntent(paymentFlowResult)
                }
            }

            result.fold(
                onSuccess = {
                    onPaymentIntentResult(it)
                },
                onFailure = { error ->
                    selection.value?.let {
                        eventReporter.onPaymentFailure(it)
                    }

                    paymentIntent.value?.let { resetViewState(it, apiThrowableToString(error)) }
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
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val application = applicationSupplier()
            val config = PaymentConfiguration.getInstance(application)
            val publishableKey = config.publishableKey
            val stripeAccountId = config.stripeAccountId
            val stripeRepository = StripeApiRepository(
                application,
                publishableKey
            )

            val starterArgs = starterArgsSupplier()

            val googlePayRepository =
                starterArgs.config?.googlePay?.environment?.let { environment ->
                    DefaultGooglePayRepository(
                        application,
                        environment
                    )
                } ?: GooglePayRepository.Disabled

            val prefsRepository = starterArgs.config?.customer?.let { (id) ->
                DefaultPrefsRepository(
                    application,
                    customerId = id,
                    isGooglePayReady = { googlePayRepository.isReady().first() },
                    workContext = Dispatchers.IO
                )
            } ?: PrefsRepository.Noop()

            val stripeIntentRepository = StripeIntentRepository.Api(
                stripeRepository = stripeRepository,
                requestOptions = ApiRequest.Options(publishableKey, stripeAccountId),
                workContext = Dispatchers.IO
            )

            val paymentMethodsRepository = PaymentMethodsApiRepository(
                stripeRepository = stripeRepository,
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                workContext = Dispatchers.IO
            )

            return PaymentSheetViewModel(
                stripeIntentRepository,
                paymentMethodsRepository,
                DefaultPaymentFlowResultProcessor(
                    application,
                    publishableKey,
                    stripeRepository,
                    enableLogging = true,
                    Dispatchers.IO
                ),
                googlePayRepository,
                prefsRepository,
                DefaultEventReporter(
                    mode = EventReporter.Mode.Complete,
                    starterArgs.sessionId,
                    application
                ),
                starterArgs,
                logger = Logger.noop(),
                workContext = Dispatchers.IO,
                application = application
            ) as T
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
