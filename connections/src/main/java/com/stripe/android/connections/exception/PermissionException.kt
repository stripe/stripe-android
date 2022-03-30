package com.stripe.android.connections.exception

import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.StripeException
import java.net.HttpURLConnection

/**
 * A type of [AuthenticationException] resulting from incorrect permissions
 * to perform the requested action.
 */
internal class PermissionException(
    stripeError: StripeError,
    requestId: String? = null
) : StripeException(
    stripeError,
    requestId,
    HttpURLConnection.HTTP_FORBIDDEN
)
