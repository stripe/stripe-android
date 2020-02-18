package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * An [Exception] indicating that too many requests have hit the API too quickly.
 */
class RateLimitException(
    stripeError: StripeError? = null,
    requestId: String? = null,
    message: String? = stripeError?.message,
    cause: Throwable? = null
) : StripeException(
    stripeError,
    requestId,
    429,
    cause,
    message
)
