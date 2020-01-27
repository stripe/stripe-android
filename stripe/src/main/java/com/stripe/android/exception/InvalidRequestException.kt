package com.stripe.android.exception

import com.stripe.android.StripeError

/**
 * An [Exception] indicating that invalid parameters were used in a request.
 */
open class InvalidRequestException(
    stripeError: StripeError? = null,
    requestId: String? = null,
    statusCode: Int = 0,
    message: String? = stripeError?.message,
    val param: String? = stripeError?.param,
    cause: Throwable? = null
) : StripeException(
    stripeError,
    requestId,
    statusCode,
    cause,
    message
) {
    val errorCode: String? = stripeError?.code
    val errorDeclineCode: String? = stripeError?.declineCode
}
