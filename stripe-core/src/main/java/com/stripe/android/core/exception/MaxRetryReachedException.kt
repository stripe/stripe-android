package com.stripe.android.core.exception

import androidx.annotation.RestrictTo

/**
 * An [Exception] that represents max retry is reached when making a request.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MaxRetryReachedException(message: String? = null) : StripeException(message = message) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = "maxRetryReachedError"
}
