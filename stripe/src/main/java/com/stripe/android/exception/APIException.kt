package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * An [Exception] that represents an internal problem with Stripe's servers.
 */
class APIException(
    stripeError: StripeError? = null,
    requestId: String? = null,
    statusCode: Int = 0,
    message: String? = stripeError?.message,
    cause: Throwable? = null
) : StripeException(
    stripeError = stripeError,
    requestId = requestId,
    statusCode = statusCode,
    cause = cause,
    message = message
) {
    internal constructor(exception: Exception) : this(
        message = exception.message,
        cause = exception
    )

    internal companion object {
        @JvmSynthetic
        internal fun create(e: CardException): APIException {
            return APIException(
                stripeError = e.stripeError,
                requestId = e.requestId,
                statusCode = e.statusCode,
                message = e.message,
                cause = e
            )
        }
    }
}
