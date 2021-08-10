package com.stripe.android.payments.paymentlauncher

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.exception.APIException
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarterHost

/**
 * WIP - view model for PaymentLauncherHostActivity
 */

internal class PaymentLauncherViewModel : ViewModel() {
    internal lateinit var paymentBrowserAuthLauncher:
        ActivityResultLauncher<PaymentBrowserAuthContract.Args>

    /**
     * [PaymentResult] live data to be observed.
     */
    internal val paymentLauncherResult = MutableLiveData<PaymentResult>()

    /**
     * Register for [ActivityResultCaller] when it's available.
     */
    internal fun registerFromActivity(activityResultCaller: ActivityResultCaller) {}

    /**
     * Unregister any [ActivityResultCaller] hosted.
     */
    internal fun unregisterFromActivity() {}

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
                paymentLauncherResult.postValue(PaymentResult.TimedOut)
            }
        }
    }

    internal class Factory(
        private val contextSupplier: () -> Context,
        private val authHostSupplier: () -> AuthActivityStarterHost,
        private val argsProvider: () -> PaymentLauncherContract.Args
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentLauncherViewModel() as T
        }
    }
}
