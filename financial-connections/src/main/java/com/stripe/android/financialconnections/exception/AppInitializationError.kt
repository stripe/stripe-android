package com.stripe.android.financialconnections.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException

class AppInitializationError internal constructor(
    message: String,
    internal val reason: Reason,
) : StripeException(
    message = message,
    cause = null,
    requestId = null,
    statusCode = 0,
    stripeError = null
) {
    constructor(message: String) : this(message, Reason.UNKNOWN)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = "fcInitializationError"

    internal enum class Reason {
        BROWSER_UNAVAILABLE,
        UNKNOWN,
    }
}
