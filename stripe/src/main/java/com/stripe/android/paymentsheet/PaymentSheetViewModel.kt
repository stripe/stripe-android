package com.stripe.android.paymentsheet

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.stripe.android.GooglePayConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.PaymentSessionPrefs
import com.stripe.android.StripeIntentResult
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.googlepay.StripeGooglePayEnvironment
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.DefaultPaymentFlowResultProcessor
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.ConfirmParamsFactory
import com.stripe.android.paymentsheet.model.PaymentIntentValidator
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetViewModel internal constructor(
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val stripeRepository: StripeRepository,
    private val paymentFlowResultProcessor: PaymentFlowResultProcessor,
    googlePayRepository: GooglePayRepository,
    prefsRepository: PrefsRepository,
    private val eventReporter: EventReporter,
    internal val args: PaymentSheetContract.Args,
    private val animateOutMillis: Long,
    workContext: CoroutineContext
) : SheetViewModel<PaymentSheetViewModel.TransitionTarget, ViewState>(
    config = args.config,
    isGooglePayEnabled = args.isGooglePayEnabled,
    googlePayRepository = googlePayRepository,
    prefsRepository = prefsRepository,
    workContext = workContext
) {
    private val confirmParamsFactory = ConfirmParamsFactory(
        args.clientSecret
    )

    private val _googlePayCompletion = MutableLiveData<PaymentIntentResult>()
    internal val googlePayCompletion: LiveData<PaymentIntentResult> = _googlePayCompletion

    private val _startConfirm = MutableLiveData<ConfirmPaymentIntentParams>()
    internal val startConfirm: LiveData<ConfirmPaymentIntentParams> = _startConfirm

    private val paymentIntentValidator = PaymentIntentValidator()

    init {
        eventReporter.onInit(config)
    }

    fun updatePaymentMethods() {
        customerConfig?.let {
            updatePaymentMethods(it)
        } ?: _paymentMethods.postValue(emptyList())
    }

    fun fetchPaymentIntent() {
        viewModelScope.launch {
            val result = withContext(workContext) {
                runCatching {
                    val paymentIntent = stripeRepository.retrievePaymentIntent(
                        args.clientSecret,
                        ApiRequest.Options(publishableKey, stripeAccountId)
                    )
                    requireNotNull(paymentIntent) {
                        "Could not parse PaymentIntent."
                    }
                }
            }
            result.fold(
                onSuccess = ::onPaymentIntentResponse,
                onFailure = {
                    _paymentIntent.value = null

                    onFatal(it)
                }
            )
        }
    }

    private fun onPaymentIntentResponse(paymentIntent: PaymentIntent) {
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

    private fun resetViewState(paymentIntent: PaymentIntent) {
        val amount = paymentIntent.amount
        val currencyCode = paymentIntent.currency
        if (amount != null && currencyCode != null) {
            _viewState.value = ViewState.Ready(amount, currencyCode)
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
                _launchGooglePay.value = StripeGooglePayContract.Args.PaymentData(
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

    private fun confirmPaymentSelection(
        paymentSelection: PaymentSelection?
    ) {
        when (paymentSelection) {
            is PaymentSelection.Saved -> {
                confirmParamsFactory.create(paymentSelection)
            }
            is PaymentSelection.New.Card -> {
                confirmParamsFactory.create(paymentSelection)
            }
            else -> null
        }?.let { confirmParams ->
            _viewState.value = ViewState.Confirming
            _startConfirm.value = confirmParams
        }
    }

    private fun onPaymentIntentResult(
        paymentIntentResult: PaymentIntentResult
    ) {
        when (paymentIntentResult.outcome) {
            StripeIntentResult.Outcome.SUCCEEDED -> {
                eventReporter.onPaymentSuccess(selection.value)

                _viewState.value = ViewState.Completed(paymentIntentResult)
            }
            else -> {
                eventReporter.onPaymentFailure(selection.value)

                val paymentIntent = paymentIntentResult.intent
                onApiError(paymentIntent.lastPaymentError?.message)
                onPaymentIntentResponse(paymentIntentResult.intent)
            }
        }
    }

    internal fun onGooglePayResult(
        googlePayResult: StripeGooglePayContract.Result
    ) {
        when (googlePayResult) {
            is StripeGooglePayContract.Result.PaymentIntent -> {
                eventReporter.onPaymentSuccess(PaymentSelection.GooglePay)
                _googlePayCompletion.value = googlePayResult.paymentIntentResult
            }
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

    @VisibleForTesting
    internal fun setPaymentMethods(paymentMethods: List<PaymentMethod>) {
        _paymentMethods.value = paymentMethods
    }

    internal fun startAnimateOut() = liveData {
        delay(animateOutMillis)
        emit(Unit)
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

    private fun updatePaymentMethods(
        customerConfig: PaymentSheet.CustomerConfiguration
    ) {
        viewModelScope.launch {
            withContext(workContext) {
                runCatching {
                    stripeRepository.getPaymentMethods(
                        ListPaymentMethodsParams(
                            customerId = customerConfig.id,
                            paymentMethodType = PaymentMethod.Type.Card
                        ),
                        publishableKey,
                        PRODUCT_USAGE,
                        ApiRequest.Options(
                            customerConfig.ephemeralKeySecret,
                            stripeAccountId
                        )
                    )
                }
            }.getOrDefault(
                emptyList()
            ).let(::setPaymentMethods)
        }
    }

    internal enum class TransitionTarget(
        val sheetMode: SheetMode
    ) {
        // User has saved PM's and is selected
        SelectSavedPaymentMethod(SheetMode.Wrapped),

        // User has saved PM's and is adding a new one
        AddPaymentMethodFull(SheetMode.Full),

        // User has no saved PM's
        AddPaymentMethodSheet(SheetMode.FullCollapsed)
    }

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> PaymentSheetContract.Args,
        private val animateOutMillis: Long
    ) : ViewModelProvider.Factory {

        internal constructor(
            applicationSupplier: () -> Application,
            starterArgsSupplier: () -> PaymentSheetContract.Args
        ) : this(
            applicationSupplier,
            starterArgsSupplier,
            animateOutMillis = ANIMATE_OUT_MILLIS
        )

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
            val googlePayRepository = DefaultGooglePayRepository(
                application,
                starterArgs.config?.googlePay?.environment
                    ?: PaymentSheet.GooglePayConfiguration.Environment.Test
            )

            val prefsRepository = starterArgs.config?.customer?.let { (id) ->
                DefaultPrefsRepository(
                    customerId = id,
                    PaymentSessionPrefs.Default(application)
                )
            } ?: PrefsRepository.Noop()

            return PaymentSheetViewModel(
                publishableKey,
                stripeAccountId,
                stripeRepository,
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
                animateOutMillis,
                Dispatchers.IO
            ) as T
        }
    }

    private companion object {
        private val PRODUCT_USAGE = setOf("PaymentSheet")

        // the delay before the payment sheet is dismissed
        private const val ANIMATE_OUT_MILLIS = 1500L
    }
}
