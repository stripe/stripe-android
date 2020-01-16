package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * An [Exception] indicating that too many requests have hit the API too quickly.
 */
class RateLimitException(
    stripeError: StripeError,
    requestId: String? = null
) : InvalidRequestException(
    stripeError = stripeError,
    requestId = requestId,
    statusCode = 429
)
