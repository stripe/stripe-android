package com.stripe.android.financialconnections.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException

class AppInitializationError(message: String) : StripeException(
    message = message,
    cause = null,
    requestId = null,
    statusCode = 0,
    stripeError = null
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = "fcInitializationError"
}
