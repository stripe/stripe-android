package com.stripe.android.core.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError
import java.net.HttpURLConnection

/**
 * A type of [AuthenticationException] resulting from incorrect permissions
 * to perform the requested action.
 */
class PermissionException(
    stripeError: StripeError,
    requestId: String? = null
) : StripeException(
    stripeError,
    requestId,
    HttpURLConnection.HTTP_FORBIDDEN
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = "permissionError"
}
