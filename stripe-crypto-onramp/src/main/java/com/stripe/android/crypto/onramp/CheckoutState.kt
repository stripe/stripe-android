package com.stripe.android.crypto.onramp

import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.model.PaymentIntent

/**
 * Represents the checkout state with session info and current status.
 */
internal data class CheckoutState(
    val onrampSessionId: String? = null,
    val checkoutHandler: (suspend () -> String)? = null,
    val status: Status = Status.Idle
) {
    internal sealed interface Status {
        /**
         * No checkout is currently in progress.
         */
        data object Idle : Status

        /**
         * Checkout is currently processing.
         */
        data object Processing : Status

        /**
         * Checkout requires next action (e.g., 3DS authentication).
         */
        data class RequiresNextAction(val paymentIntent: PaymentIntent) :
            Status

        /**
         * Checkout completed with a final result.
         */
        data class Completed(val result: OnrampCheckoutResult) : Status
    }
}
