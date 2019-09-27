package com.stripe.android.exception

import com.stripe.android.StripeError
import java.net.HttpURLConnection

/**
 * An [Exception] that represents a failure to authenticate yourself to the server.
 */
open class AuthenticationException internal constructor(
    message: String?,
    requestId: String?,
    statusCode: Int,
    stripeError: StripeError?
) : StripeException(stripeError, message, requestId, statusCode) {

    constructor(
        message: String?,
        requestId: String?,
        stripeError: StripeError?
    ) : this(message, requestId, HttpURLConnection.HTTP_UNAUTHORIZED, stripeError)
}
