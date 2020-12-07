package com.stripe.android.paymentsheet

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.PaymentSessionPrefs
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripePaymentController
import com.stripe.android.googlepay.StripeGooglePayLauncher
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.model.ConfirmParamsFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class PaymentSheetViewModel internal constructor(
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val stripeRepository: StripeRepository,
    private val paymentController: PaymentController,
    googlePayRepository: GooglePayRepository,
    private val prefsRepository: PrefsRepository,
    internal val args: PaymentSheetActivityStarter.Args,
    workContext: CoroutineContext
) : SheetViewModel<PaymentSheetViewModel.TransitionTarget, ViewState>(
    isGuestMode = args is PaymentSheetActivityStarter.Args.Guest,
    isGooglePayEnabled = args.isGooglePayEnabled,
    googlePayRepository = googlePayRepository,
    workContext = workContext
) {
    private val confirmParamsFactory = ConfirmParamsFactory()

    fun updatePaymentMethods() {
        when (args) {
            is PaymentSheetActivityStarter.Args.Default -> {
                updatePaymentMethods(args.customerConfiguration)
            }
            is PaymentSheetActivityStarter.Args.Guest -> {
                mutablePaymentMethods.postValue(emptyList())
            }
        }
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
                    mutablePaymentIntent.value = null

                    onFatal(it)
                }
            )
        }
    }

    private fun onPaymentIntentResponse(paymentIntent: PaymentIntent) {
        if (paymentIntent.confirmationMethod == PaymentIntent.ConfirmationMethod.Automatic) {
            mutablePaymentIntent.value = paymentIntent
            resetViewState(paymentIntent)
        } else {
            onFatal(
                IllegalArgumentException(
                    """
                    PaymentIntent with confirmation_method='automatic' is required.
                    See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-confirmation_method.
                    """.trimIndent()
                )
            )
        }
    }

    private fun resetViewState(paymentIntent: PaymentIntent) {
        val amount = paymentIntent.amount
        val currencyCode = paymentIntent.currency
        if (amount != null && currencyCode != null) {
            mutableViewState.value = ViewState.Ready(amount, currencyCode)
        } else {
            // TODO(mshafrir-stripe): improve error message
            onFatal(IllegalStateException("PaymentIntent is invalid."))
        }
    }

    fun checkout(activity: Activity) {
        mutableUserMessage.value = null
        mutableProcessing.value = true

        prefsRepository.savePaymentSelection(selection.value)

        if (selection.value == PaymentSelection.GooglePay) {
            paymentIntent.value?.let { paymentIntent ->
                StripeGooglePayLauncher(activity).startForResult(
                    StripeGooglePayLauncher.Args(
                        paymentIntent = paymentIntent,
                        countryCode = args.googlePayConfig?.countryCode.orEmpty()
                    )
                )
            }
        } else {
            val confirmParams = selection.value?.let { paymentSelection ->
                confirmParamsFactory.create(
                    args.clientSecret,
                    paymentSelection,
                    shouldSavePaymentMethod
                )
            }

            when {
                confirmParams != null -> {
                    mutableViewState.value = ViewState.Confirming
                    paymentController.startConfirmAndAuth(
                        AuthActivityStarter.Host.create(activity),
                        confirmParams,
                        ApiRequest.Options(
                            apiKey = publishableKey,
                            stripeAccount = stripeAccountId
                        )
                    )
                }
                else -> {
                    onFatal(
                        IllegalStateException("checkout called when no payment method selected")
                    )
                }
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        data?.takeIf { paymentController.shouldHandlePaymentResult(requestCode, it) }?.let {
            paymentController.handlePaymentResult(
                it,
                object : ApiResultCallback<PaymentIntentResult> {
                    override fun onSuccess(result: PaymentIntentResult) {
                        onPaymentIntentResult(result)
                    }

                    override fun onError(e: Exception) {
                        onApiError(e.message)
                        paymentIntent.value?.let(::resetViewState)
                    }
                }
            )
        }

        if (requestCode == StripeGooglePayLauncher.REQUEST_CODE &&
            resultCode == Activity.RESULT_OK && data != null
        ) {
            onGooglePayResult(data)
        }
    }

    private fun onPaymentIntentResult(paymentIntentResult: PaymentIntentResult) {
        when (paymentIntentResult.outcome) {
            StripeIntentResult.Outcome.SUCCEEDED -> {
                mutableViewState.value = ViewState.Completed(paymentIntentResult)
            }
            else -> {
                val paymentIntent = paymentIntentResult.intent
                onApiError(paymentIntent.lastPaymentError?.message)
                onPaymentIntentResponse(paymentIntentResult.intent)
            }
        }
    }

    private fun onGooglePayResult(data: Intent) {
        val googlePayResult = StripeGooglePayLauncher.Result.fromIntent(data)
        when (googlePayResult) {
            is StripeGooglePayLauncher.Result.PaymentIntent -> {
                mutableViewState.value = ViewState.Completed(googlePayResult.paymentIntentResult)
            }
            else -> {
                // TODO(mshafrir-stripe): handle error
            }
        }
    }

    @VisibleForTesting
    internal fun setPaymentMethods(paymentMethods: List<PaymentMethod>) {
        mutablePaymentMethods.postValue(paymentMethods)
    }

    private fun updatePaymentMethods(
        customerConfig: PaymentSheet.CustomerConfiguration,
        stripeAccountId: String? = this.stripeAccountId
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
            }.fold(
                onSuccess = this@PaymentSheetViewModel::setPaymentMethods,
                onFailure = ::onFatal
            )
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
        private val starterArgsSupplier: () -> PaymentSheetActivityStarter.Args
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
            val paymentController = StripePaymentController(
                application,
                publishableKey,
                stripeRepository,
                true
            )
            val googlePayRepository = DefaultGooglePayRepository(application)

            val starterArgs = starterArgsSupplier()

            val prefsRepository = when (starterArgs) {
                is PaymentSheetActivityStarter.Args.Default -> {
                    DefaultPrefsRepository(
                        starterArgs.customerConfiguration.id,
                        PaymentSessionPrefs.Default(application)
                    )
                }
                is PaymentSheetActivityStarter.Args.Guest -> {
                    PrefsRepository.Noop()
                }
            }

            return PaymentSheetViewModel(
                publishableKey,
                stripeAccountId,
                stripeRepository,
                paymentController,
                googlePayRepository,
                prefsRepository,
                starterArgs,
                Dispatchers.IO
            ) as T
        }
    }

    private companion object {
        private val PRODUCT_USAGE = setOf("PaymentSheet")
    }
}
