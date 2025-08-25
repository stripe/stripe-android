package com.stripe.android.crypto.onramp

import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.model.PaymentIntent

/**
 * Represents the checkout state with current status.
 */
internal data class CheckoutState(
    val status: Status
) {
    internal sealed interface Status {
        /**
         * Checkout is currently processing.
         */
        data class Processing(
            val onrampSessionId: String,
            val checkoutHandler: suspend () -> String,
            val platformKey: String,
        ) : Status

        /**
         * Checkout requires next action (e.g., 3DS authentication).
         */
        data class RequiresNextAction(
            val onrampSessionId: String,
            val checkoutHandler: suspend () -> String,
            val paymentIntent: PaymentIntent,
            val platformKey: String
        ) : Status

        /**
         * Checkout completed with a final result.
         */
        data class Completed(val result: OnrampCheckoutResult) : Status
    }
}
