package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * An [Exception] indicating that invalid parameters were used in a request.
 */
open class InvalidRequestException(
    message: String?,
    val param: String?,
    requestId: String?,
    statusCode: Int,
    val errorCode: String?,
    val errorDeclineCode: String?,
    stripeError: StripeError?,
    e: Throwable?
) : StripeException(
    stripeError,
    message,
    requestId,
    statusCode,
    e
)
