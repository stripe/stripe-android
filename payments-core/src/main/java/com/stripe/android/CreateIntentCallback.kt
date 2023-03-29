package com.stripe.android

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface AbsCreateIntentCallback

/**
 * Callback to be used when you use `PaymentSheet` and intend to create the [PaymentIntent] or
 * [SetupIntent] on your server.
 */
@ExperimentalPaymentSheetDecouplingApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CreateIntentCallback : AbsCreateIntentCallback {

    /**
     * Called when the customer confirms the payment or setup.
     *
     * Your implementation should create a [PaymentIntent] or [SetupIntent] on your server and return
     * its client secret an error if one was occurred.
     *
     * @param paymentMethodId The ID of the PaymentMethod representing the customer's payment details
     */
    suspend fun onCreateIntent(paymentMethodId: String): Result

    /**
     * Represents the result of a [CreateIntentCallback] or
     * [CreateIntentCallbackForServerSideConfirmation].
     */
    @ExperimentalPaymentSheetDecouplingApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {

        @ExperimentalPaymentSheetDecouplingApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Success(val clientSecret: String) : Result

        @ExperimentalPaymentSheetDecouplingApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Failure(
            internal val cause: Exception,
            internal val displayMessage: String? = null
        ) : Result
    }
}

/**
 * Callback to be used when you use `PaymentSheet` and intend to create and confirm the
 * [PaymentIntent] or [SetupIntent] on your server.
 */
@ExperimentalPaymentSheetDecouplingApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CreateIntentCallbackForServerSideConfirmation : AbsCreateIntentCallback {

    /**
     * Called when the customer confirms the payment or setup.
     *
     * Your implementation should create and confirm a [PaymentIntent] or [SetupIntent] on your
     * server and return its client secret an error if one was occurred.
     *
     * @param paymentMethodId The ID of the PaymentMethod representing the customer's payment details
     * @param shouldSavePaymentMethod This is [true] if the customer selected the
     * "Save this payment method for future use" checkbox. Set `setup_future_usage` on the
     * [PaymentIntent] to `off_session` if this is [true].
     */
    suspend fun onCreateIntent(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
    ): CreateIntentCallback.Result
}
