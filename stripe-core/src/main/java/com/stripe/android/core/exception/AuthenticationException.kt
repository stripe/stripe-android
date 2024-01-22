package com.stripe.android.core.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError
import java.net.HttpURLConnection

/**
 * No valid API key provided.
 *
 * [Errors](https://stripe.com/docs/api/errors)
 */
class AuthenticationException
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    stripeError: StripeError,
    requestId: String? = null
) : StripeException(
    stripeError,
    requestId,
    HttpURLConnection.HTTP_UNAUTHORIZED
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = "authError"
}
