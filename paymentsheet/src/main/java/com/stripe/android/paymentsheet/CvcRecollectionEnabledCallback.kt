package com.stripe.android.paymentsheet

import androidx.annotation.RestrictTo

/**
 * Callback to be used when you use [PaymentSheet] or [PaymentSheet.FlowController] and intend to
 * create a [PaymentIntent] on your server and confirm on the client.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalCvcRecollectionApi
fun interface CvcRecollectionEnabledCallback {
    /**
     * Your implementation should include the business logic needed to determine if CVC Recollection is
     * required for payment with a saved card.
     */
    fun isCvcRecollectionRequired(): Boolean
}
