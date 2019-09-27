package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * An [Exception] indicating that too many requests have hit the API too quickly.
 */
class RateLimitException(
    message: String?,
    param: String?,
    requestId: String?,
    stripeError: StripeError?
) : InvalidRequestException(message, param, requestId, 429, null, null, stripeError, null)
