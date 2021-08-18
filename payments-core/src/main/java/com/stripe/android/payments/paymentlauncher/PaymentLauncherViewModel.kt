package com.stripe.android.payments.paymentlauncher

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.StripeIntentResult
import com.stripe.android.exception.APIException
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.StripeIntent

/**
 * WIP - view model for PaymentLauncherHostActivity
 */

internal class PaymentLauncherViewModel : ViewModel() {
    /**
     * [PaymentResult] live data to be observed.
     */
    internal val paymentLauncherResult = MutableLiveData<PaymentResult>()

    /**
     * Confirms a payment intent or setup intent
     */
    internal fun confirmStripeIntent(confirmStripeIntentParams: ConfirmStripeIntentParams) {}

    /**
     * Fetches a [StripeIntent] and handles its next action.
     */
    internal fun handleNextActionForStripeIntent(clientSecret: String) {}

    /**
     * Parse [StripeIntentResult] into [PaymentResult].
     */
    private fun postResult(stripeIntentResult: StripeIntentResult<StripeIntent>) {
        when (stripeIntentResult.outcome) {
            StripeIntentResult.Outcome.SUCCEEDED -> {
                paymentLauncherResult.postValue(PaymentResult.Completed)
            }
            StripeIntentResult.Outcome.FAILED -> {
                paymentLauncherResult.postValue(
                    PaymentResult.Failed(
                        APIException(message = stripeIntentResult.failureMessage)
                    )
                )
            }
            StripeIntentResult.Outcome.CANCELED -> {
                paymentLauncherResult.postValue(PaymentResult.Canceled)
            }
            StripeIntentResult.Outcome.TIMEDOUT -> {
                PaymentResult.Failed(
                    APIException(message = TIMEOUT_ERROR + stripeIntentResult.failureMessage)
                )
            }
        }
    }

    internal class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentLauncherViewModel() as T
        }
    }

    companion object {
        const val TIMEOUT_ERROR = "Payment fails due to time out. \n"
    }
}
