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
) : PaymentLauncher {
    override fun confirm(params: ConfirmPaymentIntentParams) {
        // start a new activity to
        // confirm the intent with stripeRepository
        // resolve the nextActionData with authenticatorRegistry
        // report result to callback
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.IntentConfirmationArgs(params)
        )
    }

    override fun confirm(params: ConfirmSetupIntentParams) {
        // start a new activity to
        // confirm the intent with stripeRepository
        // resolve the nextActionData with authenticatorRegistry
        // report result to callback
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.IntentConfirmationArgs(params)
        )
    }

    override fun handleNextActionForPaymentIntent(clientSecret: String) {
        // start a new activity to
        // fetch the intent with stripeRepository
        // resolve the nextActionData with authenticatorRegistry
        // report result to callback
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.PaymentIntentNextActionArgs(
                paymentIntentClientSecret = clientSecret
            )
        )
    }

    override fun handleNextActionForSetupIntent(clientSecret: String) {
        // start a new activity to
        // fetch the intent with stripeRepository
        // resolve the nextActionData with authenticatorRegistry
        // report result to callback
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.SetupIntentNextActionArgs(
                setupIntentClientSecret = clientSecret
            )
        )
    }
}
