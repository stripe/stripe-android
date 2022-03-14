package com.stripe.android.payments.paymentlauncher

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import com.stripe.android.StripeApiBeta
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent

/**
 * API to confirm and handle next actions for [PaymentIntent] and [SetupIntent].
 */
interface PaymentLauncher {
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
        /**
         * Create a [PaymentLauncher] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the [ComponentActivity], it needs
         * to be called before the [ComponentActivity] is created. Use this API to access specific
         * beta features.
         */
        fun create(
            activity: ComponentActivity,
            publishableKey: String,
            stripeAccountId: String? = null,
            betas: Set<StripeApiBeta> = setOf(),
            callback: PaymentResultCallback,
        ) = PaymentLauncherFactory(activity, callback).create(publishableKey, stripeAccountId, betas)

        /**
         * Create a [PaymentLauncher] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment]'s hosting Activity, it
         * needs to be called before the [Fragment] is created. Use this API to access specific beta
         * features.
         */
        fun create(
            fragment: Fragment,
            publishableKey: String,
            stripeAccountId: String? = null,
            betas: Set<StripeApiBeta> = setOf(),
            callback: PaymentResultCallback
        ) = PaymentLauncherFactory(fragment, callback).create(publishableKey, stripeAccountId, betas)

        /**
         * Create a [PaymentLauncher] used for Jetpack Compose.
         *
         * This API uses Compose specific API [rememberLauncherForActivityResult] to register a
         * [ActivityResultLauncher] into current activity, it should be called as part of Compose
         * initialization path. Use this API to access specific beta features.
         */
        @Composable
        fun createForCompose(
            publishableKey: String,
            stripeAccountId: String? = null,
            betas: Set<StripeApiBeta> = setOf(),
            callback: PaymentResultCallback
        ) = PaymentLauncherFactory(
            LocalContext.current,
            rememberLauncherForActivityResult(
                PaymentLauncherContract(),
                callback::onPaymentResult
            )
        ).create(publishableKey, stripeAccountId, betas)
    }
}
