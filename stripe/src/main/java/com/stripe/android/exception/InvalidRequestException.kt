package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * An [Exception] indicating that invalid parameters were used in a request.
 */
open class InvalidRequestException(
    message: String? = null,
    val param: String? = null,
    requestId: String? = null,
    statusCode: Int = 0,
    val errorCode: String? = null,
    val errorDeclineCode: String? = null,
    stripeError: StripeError? = null,
    e: Throwable? = null
) : StripeException(
    stripeError,
    message,
    requestId,
    statusCode,
    e
)
