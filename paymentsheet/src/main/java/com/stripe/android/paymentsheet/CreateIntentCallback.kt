package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent

/**
 * Callback to be used when you use `PaymentSheet` and intend to create and optionally confirm the
 * [PaymentIntent] or [SetupIntent] on your server.
 */
fun interface CreateIntentCallback {

    /**
     * Called when the customer confirms the payment or setup.
     *
     * Your implementation should create and optionally confirm a [PaymentIntent] or [SetupIntent]
     * on your server and return its client secret or an error if one occurred.
     *
     * @param paymentMethod The [PaymentMethod] representing the customer's payment details. If your
     * server needs the payment method, send [PaymentMethod.id] to your server and have it fetch the
     * [PaymentMethod] object. Otherwise, you can ignore this. Don't send other properties besides
     * the ID to your server.
     * @param shouldSavePaymentMethod This is `true` if
     * - The customer selected the "Save this payment method for future use" checkbox.
     * - The [IntentConfiguration.Mode.Payment.setupFutureUse] is set to [SetupFutureUse.OffSession] or
     *   [SetupFutureUse.OnSession].
     * - The [IntentConfiguration.Mode.Payment.PaymentMethodOptions.setupFutureUsageValues] has
     *   [SetupFutureUse.OffSession] or [SetupFutureUse.OnSession] for the [PaymentMethod.Type].
     *   Set `setup_future_usage` on the [PaymentIntent] to `off_session` if this is `true`.
     */
    suspend fun onCreateIntent(
        paymentMethod: PaymentMethod,
        shouldSavePaymentMethod: Boolean,
        onBehalfOf: String?,
    ): CreateIntentResult
}

/**
 * Represents the result of a [CreateIntentCallback].
 */
sealed interface CreateIntentResult {

    class Success(internal val clientSecret: String) : CreateIntentResult

    class Failure @JvmOverloads constructor(
        internal val cause: Exception,
        internal val displayMessage: String? = null,
    ) : CreateIntentResult
}
