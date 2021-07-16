package com.stripe.android.payments.paymentlauncher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.model.ConfirmStripeIntentParams

/**
 * WIP - view model for PaymentLauncherHostActivity
 */
internal class PaymentLauncherViewModel : ViewModel() {

    /**
     * Confirms a payment intent or setup intent
     */
    fun confirmStripeIntent(confirmStripeIntentParams: ConfirmStripeIntentParams) {}

    /**
     * Fetches a payment intent and handles its next action.
     */
    fun handleNextActionForPaymentIntent(clientSecret: String) {}

    /**
     * Fetches a setup intent and handles its next action.
     */
    fun handleNextActionForSetupIntent(clientSecret: String) {}

    internal class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentLauncherViewModel() as T
        }
    }
}
