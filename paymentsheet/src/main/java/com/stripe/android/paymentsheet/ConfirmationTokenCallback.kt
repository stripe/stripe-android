package com.stripe.android.paymentsheet

import com.stripe.android.model.ConfirmationToken

/**
 * Callback to be used when you use `PaymentSheet` and intend to create ConfirmationTokens
 * for server-side payment confirmation.
 *
 * This is the ConfirmationToken equivalent of [CreateIntentCallback].
 */
fun interface CreateIntentWithConfirmationTokenCallback {

    /**
     * Called when the customer confirms the payment or setup and a ConfirmationToken is ready.
     *
     * Your implementation should create and optionally confirm a PaymentIntent or SetupIntent
     * on your server using the [confirmationToken] and return its client secret or an error if one occurred.
     *
     * @param confirmationToken The [ConfirmationToken] containing payment method data, shipping details,
     * and other checkout state collected by PaymentSheet. Send this token to your server to complete
     * payment confirmation using the Stripe server-side API.
     */
    suspend fun onCreateIntent(
        confirmationToken: ConfirmationToken,
    ): CreateIntentResult
}

/**
 * Callback to receive the result of ConfirmationToken creation.
 *
 * Used with PaymentSheet.ConfirmationTokenBuilder and FlowController when operating in
 * ConfirmationToken mode, where the SDK generates tokens instead of completing payments directly.
 */
fun interface ConfirmationTokenCallback {

    /**
     * Called with the result of ConfirmationToken creation.
     *
     * @param confirmationTokenResult The [ConfirmationTokenResult] indicating whether the
     * ConfirmationToken was created successfully, failed, or was canceled by the user.
     */
    fun onConfirmationTokenResult(
        confirmationTokenResult: ConfirmationTokenResult
    )
}
