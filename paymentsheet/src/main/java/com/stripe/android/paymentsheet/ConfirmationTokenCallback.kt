package com.stripe.android.paymentsheet

import com.stripe.android.model.ConfirmationToken

/**
 * Callback to be used when you use `PaymentSheet` and intend to create ConfirmationTokens
 * for server-side payment confirmation.
 *
 * This callback is called with the ConfirmationToken result and allows merchants to process
 * the token on their server, then return a CreateIntentResult to continue the payment flow.
 * The SDK will then call confirmationHandler.start() with the returned client secret.
 */
fun interface ConfirmationTokenCallback {

    /**
     * Called with the ConfirmationToken result when the customer confirms the payment or setup.
     *
     * @param confirmationTokenResult The [ConfirmationTokenResult] containing the ConfirmationToken
     * or error information. When successful, your implementation should:
     * 1. Send the ConfirmationToken to your server
     * 2. Create a PaymentIntent or SetupIntent using the server-side API
     * 3. Return the client secret wrapped in CreateIntentResult.Success
     * 
     * The SDK will then automatically call confirmationHandler.start() to handle 3DS and other
     * authentication flows.
     *
     * @return CreateIntentResult.Success with client secret, or CreateIntentResult.Failure if an error occurred.
     */
    suspend fun onConfirmationTokenResult(
        confirmationTokenResult: ConfirmationTokenResult
    ): CreateIntentResult
}
