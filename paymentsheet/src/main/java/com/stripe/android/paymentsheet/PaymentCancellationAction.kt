package com.stripe.android.paymentsheet

/**
 * Action to perform if a user cancels a running confirmation process.
 */
internal enum class PaymentCancellationAction {
    /**
     * This actions means the user has cancels a critical confirmation step and that the user should be notified
     * of the cancellation if relevant.
     */
    InformCancellation,

    /**
     * This action means that the user has asked to modify the payment details of their selected payment option.
     */
    ModifyPaymentDetails,

    /**
     * Means no action should be taken if the user cancels a step in the confirmation process.
     */
    None,
}
