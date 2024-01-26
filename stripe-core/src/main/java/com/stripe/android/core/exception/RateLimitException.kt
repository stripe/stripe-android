package com.stripe.android.core.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError
import com.stripe.android.core.networking.HTTP_TOO_MANY_REQUESTS

/**
 * An [Exception] indicating that too many requests have hit the API too quickly.
 */
class RateLimitException(
    stripeError: StripeError? = null,
    requestId: String? = null,
    message: String? = stripeError?.message,
    cause: Throwable? = null
) : StripeException(
    stripeError,
    requestId,
    HTTP_TOO_MANY_REQUESTS,
    cause,
    message
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = "rateLimitError"
}
