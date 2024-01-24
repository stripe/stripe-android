@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.core.exception

import androidx.annotation.RestrictTo

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val Throwable.safeAnalyticsMessage: String
    get() = when (this) {
        is StripeException -> analyticsValue() ?: javaClass.name
        else -> javaClass.name
    }
