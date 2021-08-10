package com.stripe.android.payments.paymentlauncher

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams

/**
 * WIP - implementation of [PaymentLauncher], start an [PaymentLauncherConfirmationActivity] to confirm and
 * handle next actions for intents.
 */
internal class StripePaymentLauncher internal constructor(
    private val hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>,
    private val publishableKey: String,
    private val stripeAccountId: String? = null
) : PaymentLauncher {
    override fun confirm(params: ConfirmPaymentIntentParams) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.IntentConfirmationArgs(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                confirmStripeIntentParams = params
            )
        )
    }

    override fun confirm(params: ConfirmSetupIntentParams) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.IntentConfirmationArgs(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                confirmStripeIntentParams = params
            )
        )
    }

    override fun handleNextActionForPaymentIntent(clientSecret: String) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.PaymentIntentNextActionArgs(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                paymentIntentClientSecret = clientSecret
            )
        )
    }

    override fun handleNextActionForSetupIntent(clientSecret: String) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.SetupIntentNextActionArgs(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
                setupIntentClientSecret = clientSecret
            )
        )
    }
}
