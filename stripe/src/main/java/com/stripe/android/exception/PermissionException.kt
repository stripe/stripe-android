package com.stripe.android.exception

import com.stripe.android.StripeError
import java.net.HttpURLConnection

/**
 * A type of [AuthenticationException] resulting from incorrect permissions
 * to perform the requested action.
 */
class PermissionException(
    stripeError: StripeError,
    requestId: String? = null
) : StripeException(
    stripeError, requestId, HttpURLConnection.HTTP_FORBIDDEN
)
