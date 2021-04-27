package com.stripe.android.paymentsheet

import android.app.Application
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
import com.stripe.android.SetupIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.googlepay.StripeGooglePayEnvironment
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.payments.DefaultPaymentFlowResultProcessor
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.ConfirmParamsFactory
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.model.ViewState
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
 * to convert a [ViewState.PaymentSheet] to a [PrimaryButton.State]
 */
internal fun ViewState.PaymentSheet.convert(): PrimaryButton.State? {
    return when (this) {
        is ViewState.PaymentSheet.Ready ->
            PrimaryButton.State.Ready
        is ViewState.PaymentSheet.StartProcessing ->
            PrimaryButton.State.StartProcessing
        is ViewState.PaymentSheet.FinishProcessing ->
            PrimaryButton.State.FinishProcessing(this.onComplete)
        else -> null
    }
}

internal class PaymentSheetViewModel internal constructor(
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val stripeIntentRepository: StripeIntentRepository,
    private val paymentMethodsRepository: PaymentMethodsRepository,
    private val paymentFlowResultProcessor: PaymentFlowResultProcessor,
    private val googlePayRepository: GooglePayRepository,
    prefsRepository: PrefsRepository,
    private val eventReporter: EventReporter,
    internal val args: PaymentSheetContract.Args,
    defaultReturnUrl: DefaultReturnUrl,
    private val logger: Logger = Logger.noop(),
    workContext: CoroutineContext,
    application: Application
) : BaseSheetViewModel<PaymentSheetViewModel.TransitionTarget>(
    application = application,
    config = args.config,
    prefsRepository = prefsRepository,
    workContext = workContext
) {
    private val confirmParamsFactory = ConfirmParamsFactory(
        defaultReturnUrl,
        args.clientSecret
    )

    private val _startConfirm = MutableLiveData<Event<ConfirmStripeIntentParams>>()
    internal val startConfirm: LiveData<Event<ConfirmStripeIntentParams>> = _startConfirm

    private val _amount = MutableLiveData<Amount>()
    internal val amount: LiveData<Amount> = _amount

    @VisibleForTesting
    internal val _viewState = MutableLiveData<ViewState.PaymentSheet>(null)
    internal val viewState: LiveData<ViewState.PaymentSheet> = _viewState.distinctUntilChanged()

    internal var checkoutIdentifier: CheckoutIdentifier = CheckoutIdentifier.SheetBottomBuy
    internal fun getButtonStateObservable(checkoutIdentifier: CheckoutIdentifier): MediatorLiveData<ViewState.PaymentSheet?> {
        val outputLiveData = MediatorLiveData<ViewState.PaymentSheet?>()
        outputLiveData.addSource(_viewState) { currentValue ->
            if (this.checkoutIdentifier == checkoutIdentifier) {
                outputLiveData.value = currentValue
            }
        }
        return outputLiveData
    }

    override var newCard: PaymentSelection.New.Card? = null

    private val stripeIntentValidator = StripeIntentValidator()
    private val currencyFormatter = CurrencyFormatter()

    init {
        fetchIsGooglePayReady()
        eventReporter.onInit(config)
    }

    fun fetchIsGooglePayReady() {
        if (isGooglePayReady.value == null) {
            if (args.isGooglePayEnabled && args.clientSecret is PaymentIntentClientSecret) {
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
                onSuccess = ::onStripeIntentResponse,
                onFailure = {
                    _stripeIntent.value = null
                    onFatal(it)
                }
            )
        }
    }

    private fun onStripeIntentResponse(stripeIntent: StripeIntent) {
        if (stripeIntent.isConfirmed) {
            onConfirmedStripeIntent(stripeIntent)
        } else {
            runCatching {
                stripeIntentValidator.requireValid(stripeIntent)
            }.fold(
                onSuccess = {
                    _stripeIntent.value = stripeIntent
                    resetViewState(stripeIntent)
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
        _viewState.value = ViewState.PaymentSheet.FinishProcessing {
            _viewState.value = ViewState.PaymentSheet.ProcessResult(
                when (stripeIntent) {
                    is PaymentIntent -> {
                        PaymentIntentResult(
                            stripeIntent,
                            StripeIntentResult.Outcome.SUCCEEDED
                        )
                    }
                    is SetupIntent -> {
                        SetupIntentResult(
                            stripeIntent,
                            StripeIntentResult.Outcome.SUCCEEDED
                        )
                    }
                    else -> throw IllegalStateException()
                }
            )
        }
    }

    private fun resetViewState(paymentIntent: PaymentIntent) {
        val amount = paymentIntent.amount
        val currencyCode = paymentIntent.currency
        if (amount != null && currencyCode != null) {
            _amount.value = Amount(amount, currencyCode)
            _viewState.value = ViewState.PaymentSheet.Ready
            _processing.value = false
            checkoutIdentifier = CheckoutIdentifier.None
        } else {
            onFatal(
                IllegalStateException("PaymentIntent could not be parsed correctly.")
            )

    private fun resetViewState(stripeIntent: StripeIntent) {
        if (stripeIntent is PaymentIntent) {
            val amount = stripeIntent.amount
            val currencyCode = stripeIntent.currency
            if (amount != null && currencyCode != null) {
                _viewState.value = ViewState.PaymentSheet.Ready(
                    getApplication<Application>().resources.getString(
                        R.string.stripe_paymentsheet_pay_button_amount,
                        currencyFormatter.format(amount, currencyCode)
                    )
                )
                _processing.value = false
            } else {
                onFatal(
                    IllegalStateException("PaymentIntent could not be parsed correctly.")
                )
            }
        } else {
            _viewState.value = ViewState.PaymentSheet.Ready(
                getApplication<Application>().resources.getString(
                    R.string.stripe_paymentsheet_setup_button_label
                )
            )
            _processing.value = false
        }
    }

    fun checkout(checkoutIdentifier: CheckoutIdentifier) {
        this.checkoutIdentifier = checkoutIdentifier
        _userMessage.value = null
        _processing.value = true
        _viewState.value = ViewState.PaymentSheet.StartProcessing

        val paymentSelection = selection.value

        if (paymentSelection is PaymentSelection.GooglePay) {
            stripeIntent.value?.let { stripeIntent ->
                _launchGooglePay.value = Event(
                    StripeGooglePayContract.Args(
                        paymentIntent = stripeIntent as PaymentIntent,
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

    private fun <T : StripeIntent> onStripeIntentResult(stripeIntentResult: StripeIntentResult<T>) {
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

                _viewState.value = ViewState.PaymentSheet.FinishProcessing {
                    _viewState.value = ViewState.PaymentSheet.ProcessResult(stripeIntentResult)
                }
            }
            else -> {
                eventReporter.onPaymentFailure(selection.value)

                onApiError(stripeIntentResult.failureMessage)
                runCatching {
                    stripeIntentValidator.requireValid(stripeIntentResult.intent)
                }.fold(
                    onSuccess = ::resetViewState,
                    onFailure = ::onFatal
                )
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
                onApiError(googlePayResult.exception)
                eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                paymentIntent.value?.let(::resetViewState)
            }
            else -> {
                stripeIntent.value?.let(::resetViewState)
            }
        }
    }

    fun onPaymentFlowResult(paymentFlowResult: PaymentFlowResult.Unvalidated) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(workContext) {
                    when (args.clientSecret) {
                        is PaymentIntentClientSecret -> {
                            paymentFlowResultProcessor.processPaymentIntent(paymentFlowResult)
                        }
                        is SetupIntentClientSecret -> {
                            paymentFlowResultProcessor.processSetupIntent(paymentFlowResult)
                        }
                    }
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

                    onApiError(error)
                    stripeIntent.value?.let(::resetViewState)
                }
            )
        }
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
                publishableKey,
                stripeAccountId,
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
                defaultReturnUrl = DefaultReturnUrl.create(application),
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
