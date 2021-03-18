package com.stripe.android.paymentsheet

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.googlepay.StripeGooglePayEnvironment
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.payments.DefaultPaymentFlowResultProcessor
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.ConfirmParamsFactory
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentIntentValidator
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.repositories.PaymentIntentRepository
import com.stripe.android.paymentsheet.repositories.PaymentMethodsApiRepository
import com.stripe.android.paymentsheet.repositories.PaymentMethodsRepository
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetViewModel internal constructor(
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val paymentIntentRepository: PaymentIntentRepository,
    private val paymentMethodsRepository: PaymentMethodsRepository,
    private val paymentFlowResultProcessor: PaymentFlowResultProcessor,
    private val googlePayRepository: GooglePayRepository,
    prefsRepository: PrefsRepository,
    private val eventReporter: EventReporter,
    internal val args: PaymentSheetContract.Args,
    private val logger: Logger = Logger.noop(),
    workContext: CoroutineContext
) : BaseSheetViewModel<PaymentSheetViewModel.TransitionTarget>(
    config = args.config,
    prefsRepository = prefsRepository,
    workContext = workContext
) {
    private val confirmParamsFactory = ConfirmParamsFactory(
        args.clientSecret
    )

    private val _startConfirm = MutableLiveData<ConfirmPaymentIntentParams>()
    internal val startConfirm: LiveData<ConfirmPaymentIntentParams> = _startConfirm

    @VisibleForTesting
    internal val _viewState = MutableLiveData<ViewState.PaymentSheet>(null)
    internal val viewState: LiveData<ViewState.PaymentSheet> = _viewState.distinctUntilChanged()

    override var newCard: PaymentSelection.New.Card? = null

    private val paymentIntentValidator = PaymentIntentValidator()

    init {
        fetchIsGooglePayReady()
        eventReporter.onInit(config)
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

    fun fetchPaymentIntent() {
        viewModelScope.launch {
            runCatching {
                paymentIntentRepository.get(args.clientSecret)
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
                    resetViewState(paymentIntent)
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
        _viewState.value = ViewState.PaymentSheet.FinishProcessing {
            _viewState.value = ViewState.PaymentSheet.ProcessResult(
                PaymentIntentResult(
                    paymentIntent,
                    StripeIntentResult.Outcome.SUCCEEDED
                )
            )
        }
    }

    private fun resetViewState(paymentIntent: PaymentIntent) {
        val amount = paymentIntent.amount
        val currencyCode = paymentIntent.currency
        if (amount != null && currencyCode != null) {
            _viewState.value = ViewState.PaymentSheet.Ready(amount, currencyCode)
            _processing.value = false
        } else {
            onFatal(
                IllegalStateException("PaymentIntent could not be parsed correctly.")
            )
        }
    }

    fun checkout() {
        _userMessage.value = null
        _processing.value = true

        val paymentSelection = selection.value
        prefsRepository.savePaymentSelection(paymentSelection)

        if (paymentSelection is PaymentSelection.GooglePay) {
            paymentIntent.value?.let { paymentIntent ->
                _launchGooglePay.value = StripeGooglePayContract.Args(
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
            _viewState.value = ViewState.PaymentSheet.StartProcessing
            _startConfirm.value = confirmParams
        }
    }

    private fun onPaymentIntentResult(paymentIntentResult: PaymentIntentResult) {
        val paymentIntent = paymentIntentResult.intent
        when (paymentIntentResult.outcome) {
            StripeIntentResult.Outcome.SUCCEEDED -> {
                eventReporter.onPaymentSuccess(selection.value)

                _viewState.value = ViewState.PaymentSheet.FinishProcessing {
                    _viewState.value = ViewState.PaymentSheet.ProcessResult(paymentIntentResult)
                }
            }
            else -> {
                eventReporter.onPaymentFailure(selection.value)

                onApiError(paymentIntentResult.failureMessage)
                runCatching {
                    paymentIntentValidator.requireValid(paymentIntent)
                }.fold(
                    onSuccess = {resetViewState(paymentIntent)},
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
                updateSelection(paymentSelection)
                confirmPaymentSelection(paymentSelection)
            }
            else -> {
                eventReporter.onPaymentFailure(PaymentSelection.GooglePay)

                // TODO(mshafrir-stripe): handle error
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

                    onApiError(error.message)
                    paymentIntent.value?.let(::resetViewState)
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

            val paymentIntentRepository = PaymentIntentRepository.Api(
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
                paymentIntentRepository,
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
                workContext = Dispatchers.IO
            ) as T
        }
    }
}
