package com.stripe.android.core.exception

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GenericStripeException(
    cause: Throwable,
    private val analyticsValue: String? = null,
) : StripeException(
    cause = cause,
    message = cause.message,
) {
    override fun analyticsValue(): String {
        return analyticsValue ?: "unknown"
    }
}
