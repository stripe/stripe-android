package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * An [Exception] that represents an internal problem with Stripe's servers.
 */
class APIException(
    message: String?,
    requestId: String? = null,
    statusCode: Int,
    stripeError: StripeError? = null,
    e: Throwable? = null
) : StripeException(stripeError, message, requestId, statusCode, e) {
    internal companion object {
        @JvmSynthetic
        internal fun create(e: StripeException): APIException {
            return APIException(
                message = e.message,
                requestId = e.requestId,
                statusCode = e.statusCode,
                e = e
            )
        }
    }
}
