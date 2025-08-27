package com.stripe.android.paymentsheet

import com.stripe.android.model.ConfirmationToken

/**
 * The result of an operation to create a ConfirmationToken.
 */
sealed class ConfirmationTokenResult {
    /**
     * The ConfirmationToken was created successfully and is ready for server-side confirmation.
     *
     * @param confirmationToken The [ConfirmationToken] containing payment method data, shipping details,
     * and other checkout state collected by PaymentSheet. Send this token to your server to complete
     * payment confirmation using the Stripe server-side API.
     */
    data class Completed(
        val confirmationToken: ConfirmationToken
    ) : ConfirmationTokenResult()

    /**
     * The ConfirmationToken creation failed.
     *
     * @param error The error that occurred during ConfirmationToken creation.
     */
    data class Failed(
        val error: Throwable
    ) : ConfirmationTokenResult()

    /**
     * The user canceled the ConfirmationToken creation flow.
     */
    data object Canceled : ConfirmationTokenResult()
}
