package com.stripe.android.core.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError

/**
 * A [StripeException] indicating that invalid parameters were used in a response.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InvalidResponseException(
    stripeError: StripeError? = null,
    requestId: String? = null,
    statusCode: Int = 0,
    message: String? = stripeError?.message,
    cause: Throwable? = null
) : StripeException(
    stripeError,
    requestId,
    statusCode,
    cause,
    message
)
