package com.stripe.android.core.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError

/**
 * An [Exception] that represents an internal problem with Stripe's servers.
 */
class APIException(
    stripeError: StripeError? = null,
    requestId: String? = null,
    statusCode: Int = 0,
    message: String? = stripeError?.message,
    cause: Throwable? = null
) : StripeException(
    stripeError = stripeError,
    requestId = requestId,
    statusCode = statusCode,
    cause = cause,
    message = message
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(throwable: Throwable) : this(
        message = throwable.message,
        cause = throwable
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = "apiError"
}
