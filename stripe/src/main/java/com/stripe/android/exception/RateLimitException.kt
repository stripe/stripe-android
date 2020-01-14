package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * An [Exception] indicating that too many requests have hit the API too quickly.
 */
class RateLimitException(
    message: String? = null,
    param: String? = null,
    requestId: String? = null,
    stripeError: StripeError? = null
) : InvalidRequestException(
    message = message,
    param = param,
    requestId = requestId,
    statusCode = 429,
    stripeError = stripeError
)
