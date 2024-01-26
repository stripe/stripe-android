@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.core.exception

import androidx.annotation.RestrictTo
import java.io.IOException

private const val IO_EXCEPTION_ANALYTICS_MESSAGE = "ioException"
private const val DEFAULT_ANALYTICS_MESSAGE = "unknown"

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val Throwable.safeAnalyticsMessage: String
    get() = when (this) {
        is StripeException -> analyticsValue()
        is IOException -> IO_EXCEPTION_ANALYTICS_MESSAGE
        else -> DEFAULT_ANALYTICS_MESSAGE
    }
