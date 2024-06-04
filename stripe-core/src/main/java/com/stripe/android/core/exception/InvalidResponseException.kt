package com.stripe.android.core.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError

/**
 * A [StripeException] indicating that invalid parameters were used in a response. E.g when the
 * response contains a null field that shouldn't be null, or contains an unknown Enum value that's
 * not defined in the SDK.
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
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = "invalidResponseError"
}
