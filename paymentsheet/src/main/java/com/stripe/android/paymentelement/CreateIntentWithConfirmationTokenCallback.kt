package com.stripe.android.paymentelement

import com.stripe.android.model.ConfirmationToken
import com.stripe.android.paymentsheet.CreateIntentResult

/**
 * Callback to be used when you intend to create ConfirmationTokens
 * for payment confirmation.
 *
 * This callback is called with the ConfirmationToken and allows merchants to process
 * the token on their server, then return a CreateIntentResult to continue the payment flow.
 */
fun interface CreateIntentWithConfirmationTokenCallback {

    /**
     * Called with the ConfirmationToken when the customer confirms the payment or setup.
     *
     * @param confirmationToken your implementation should:
     * 1. Send the ConfirmationToken to your server
     * 2. Create a PaymentIntent or SetupIntent using the server-side API
     * 3. Return the client secret wrapped in CreateIntentResult.Success
     *
     * @return CreateIntentResult.Success with client secret, or CreateIntentResult.Failure if an error occurred.
     */
    suspend fun onCreateIntent(
        confirmationToken: ConfirmationToken
    ): CreateIntentResult
}
