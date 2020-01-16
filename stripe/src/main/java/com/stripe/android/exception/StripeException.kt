package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * A base class for Stripe-related exceptions.
 */
abstract class StripeException(
    val stripeError: StripeError? = null,
    val requestId: String? = null,
    val statusCode: Int = 0,
    e: Throwable? = null,
    message: String? = stripeError?.message
) : Exception(message, e) {
    override fun toString(): String {
        return listOfNotNull(
            requestId?.let { "Request-id: $it" },
            super.toString()
        ).joinToString(separator = "\n")
    }
}
