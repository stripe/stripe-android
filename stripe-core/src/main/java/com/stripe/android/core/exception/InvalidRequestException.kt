package com.stripe.android.core.exception

import com.stripe.android.core.StripeError

/**
 * A [StripeException] indicating that invalid parameters were used in a request.
 */
class InvalidRequestException(
    stripeError: StripeError? = null,
    requestId: String? = null,
    statusCode: Int = 0,
    message: String? = stripeError?.message,
    cause: Throwable? = null
) : StripeException(
    stripeError,
    requestId,
    statusCode,
    cause,
    message
) {
    override fun analyticsValue(): String = "invalidRequestError"
}
