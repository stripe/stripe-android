package com.stripe.android.payments.paymentlauncher

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.fragment.app.Fragment
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.utils.rememberActivityOrNull

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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface InternalPaymentResultCallback {
        fun onPaymentResult(launcherResult: InternalPaymentResult)
    }

    companion object {
        /**
         * Create a [PaymentLauncher] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the [ComponentActivity],  it needs
         * to be called before the [ComponentActivity] is created.
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            activity: ComponentActivity,
            publishableKey: String,
            stripeAccountId: String? = null,
            callback: PaymentResultCallback
        ) = PaymentLauncherFactory(
            activity,
            callback.toInternalResultCallback()
        ).create(publishableKey, stripeAccountId)

        /**
         * Create a [PaymentLauncher] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment]'s hosting Activity, it
         * needs to be called before the [Fragment] is created.
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            fragment: Fragment,
            publishableKey: String,
            stripeAccountId: String? = null,
            callback: PaymentResultCallback
        ) = PaymentLauncherFactory(
            fragment,
            callback.toInternalResultCallback()
        ).create(publishableKey, stripeAccountId)

        /**
         * Create a [PaymentLauncher] used for Jetpack Compose.
         *
         * This API uses Compose specific API [rememberLauncherForActivityResult] to register a
         * [ActivityResultLauncher] into current activity, it should be called as part of Compose
         * initialization path.
         * This method creates a new PaymentLauncher object every time it is called, even during
         * recompositions.
         */
        @Deprecated(
            message = "Use rememberPaymentLauncher() instead",
            replaceWith = ReplaceWith(
                expression = "rememberPaymentLauncher(publishableKey, stripeAccountId, callback)",
            ),
        )
        @Composable
        fun createForCompose(
            publishableKey: String,
            stripeAccountId: String? = null,
            callback: PaymentResultCallback
        ): PaymentLauncher {
            return rememberPaymentLauncher(publishableKey, stripeAccountId, callback)
        }

        /**
         * Create a [PaymentLauncher] used for Jetpack Compose.
         *
         * This API uses Compose specific API [rememberLauncherForActivityResult] to register a
         * [ActivityResultLauncher] into current activity, it should be called as part of Compose
         * initialization path.
         * The PaymentLauncher created is remembered across recompositions. Recomposition will
         * always return the value produced by composition.
         */
        @Deprecated(
            message = "Use rememberPaymentLauncher() instead",
            replaceWith = ReplaceWith(
                expression = "rememberPaymentLauncher(publishableKey, stripeAccountId, callback)",
            ),
        )
        @Composable
        fun rememberLauncher(
            publishableKey: String,
            stripeAccountId: String? = null,
            callback: PaymentResultCallback
        ): PaymentLauncher {
            return rememberPaymentLauncher(publishableKey, stripeAccountId, callback)
        }
    }
}

/**
 * Creates a [PaymentLauncher] that is remembered across compositions.
 *
 * This *must* be called unconditionally, as part of the initialization path.
 *
 * @param publishableKey The publishable key to use
 * @param stripeAccountId Optional, the Connect account to associate with this request
 * @param callback Called with the result of the payment operation
 */
@Composable
fun rememberPaymentLauncher(
    publishableKey: String,
    stripeAccountId: String? = null,
    callback: PaymentLauncher.PaymentResultCallback
): PaymentLauncher {
    val activity = rememberActivityOrNull()

    val launcherCallback = remember(callback) {
        callback.toInternalResultCallback()
    }

    val activityResultLauncher = rememberLauncherForActivityResult(
        PaymentLauncherContract(),
        launcherCallback::onPaymentResult
    )

    return remember(publishableKey, stripeAccountId) {
        PaymentLauncherFactory(
            hostActivityLauncher = activityResultLauncher,
            statusBarColor = activity?.window?.statusBarColor,
        ).create(publishableKey, stripeAccountId)
    }
}
