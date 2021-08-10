package com.stripe.android.payments.paymentlauncher

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent

/**
 * API to confirm and handle next actions for [PaymentIntent] and [SetupIntent].
 */
internal interface PaymentLauncher {
    /**
     * Confirms and, if necessary, authenticates a [PaymentIntent].
     */
    fun confirm(params: ConfirmPaymentIntentParams)

    /**
     * Confirms and, if necessary, authenticates a [SetupIntent].
     */
    fun confirm(params: ConfirmSetupIntentParams)

    /**
     * Fetches a [PaymentIntent] and handles its next action.
     */
    fun handleNextActionForPaymentIntent(clientSecret: String)

    /**
     * Fetches a [SetupIntent] and handles its next action.
     */
    fun handleNextActionForSetupIntent(clientSecret: String)

    /**
     * Callback to notify when the intent is confirmed and next action handled.
     */
    fun interface PaymentResultCallback {
        fun onPaymentResult(paymentResult: PaymentResult)
    }

    companion object {
        fun create(
            activity: ComponentActivity,
            callback: PaymentResultCallback,
            publishableKey: String,
            stripeAccountId: String? = null
        ) = PaymentLauncherFactory(activity, callback).create(publishableKey, stripeAccountId)

        fun create(
            fragment: Fragment,
            callback: PaymentResultCallback,
            publishableKey: String,
            stripeAccountId: String? = null
        ) = PaymentLauncherFactory(fragment, callback).create(publishableKey, stripeAccountId)
    }
}
