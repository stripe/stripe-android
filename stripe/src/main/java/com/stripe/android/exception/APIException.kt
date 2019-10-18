package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * An [Exception] that represents an internal problem with Stripe's servers.
 */
class APIException(
    message: String?,
    requestId: String? = null,
    statusCode: Int,
    stripeError: StripeError?,
    e: Throwable? = null
) : StripeException(stripeError, message, requestId, statusCode, e)
