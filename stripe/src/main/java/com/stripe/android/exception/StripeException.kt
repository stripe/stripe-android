package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * A base class for Stripe-related [Exceptions][Exception].
 */
abstract class StripeException @JvmOverloads constructor(
    val stripeError: StripeError?,
    message: String?,
    val requestId: String?,
    val statusCode: Int,
    e: Throwable? = null
) : Exception(message, e) {

    constructor(
        message: String?,
        requestId: String?,
        statusCode: Int,
        e: Throwable?
    ) : this(null, message, requestId, statusCode, e)

    override fun toString(): String {
        val reqIdStr: String = if (requestId != null) {
            "; request-id: $requestId"
        } else {
            ""
        }
        return super.toString() + reqIdStr
    }

    internal companion object {
        protected const val serialVersionUID = 1L
    }
}
