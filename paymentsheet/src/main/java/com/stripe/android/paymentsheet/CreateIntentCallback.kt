package com.stripe.android.paymentsheet

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface AbsCreateIntentCallback

/**
 * Callback to be used when you use `PaymentSheet` and intend to create and optionally confirm the
 * [PaymentIntent] or [SetupIntent] on your server.
 */
@ExperimentalPaymentSheetDecouplingApi
fun interface CreateIntentCallback : AbsCreateIntentCallback {

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
     * @param shouldSavePaymentMethod This is `true` if the customer selected the
     * "Save this payment method for future use" checkbox. Set `setup_future_usage` on the
     * [PaymentIntent] to `off_session` if this is `true`.
     */
    suspend fun onCreateIntent(
        paymentMethod: PaymentMethod,
        shouldSavePaymentMethod: Boolean,
    ): CreateIntentResult
}

/**
 * Represents the result of a [CreateIntentCallback].
 */
@ExperimentalPaymentSheetDecouplingApi
sealed interface CreateIntentResult {

    @ExperimentalPaymentSheetDecouplingApi
    class Success(internal val clientSecret: String) : CreateIntentResult

    @ExperimentalPaymentSheetDecouplingApi
    class Failure @JvmOverloads constructor(
        internal val cause: Exception,
        internal val displayMessage: String? = null,
    ) : CreateIntentResult
}
