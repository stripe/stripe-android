package com.stripe.android.core.exception

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LocalStripeException(
    val displayMessage: String?,
    val analyticsValue: String?,
) : StripeException(
    message = displayMessage
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = analyticsValue ?: "unknown"
}
