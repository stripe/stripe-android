package com.stripe.android.paymentsheet

/**
 * Callback to be used when you use [PaymentSheet] or [PaymentSheet.FlowController] and intend to
 * create a [PaymentIntent] on your server and confirm on the client.
 */
@ExperimentalCvcRecollectionApi
fun interface CvcRecollectionEnabledCallback {
    /**
     * Your implementation should include the business logic needed to determine if CVC Recollection is
     * required for payment with a saved card.
     */
    fun isCvcRecollectionRequired(): Boolean
}
