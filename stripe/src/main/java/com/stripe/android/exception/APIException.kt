package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * An [Exception] that represents an internal problem with Stripe's servers.
 */
class APIException(
    message: String?,
    requestId: String?,
    statusCode: Int,
    stripeError: StripeError?,
    e: Throwable?
) : StripeException(stripeError, message, requestId, statusCode, e)
