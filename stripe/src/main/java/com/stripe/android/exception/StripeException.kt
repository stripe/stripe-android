package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * A base class for Stripe-related [Exceptions][Exception].
 */
abstract class StripeException @JvmOverloads constructor(
    val stripeError: StripeError? = null,
    message: String? = null,
    val requestId: String? = null,
    val statusCode: Int,
    e: Throwable? = null
) : Exception(message, e) {
    override fun toString(): String {
        return super.toString() +
            requestId?.let { "; request-id: $it" }.orEmpty()
    }
}
