package com.stripe.android.core.networking

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface AnalyticsRequestExecutor {
    /**
     * Execute the fire-and-forget request asynchronously.
     */
    fun executeAsync(request: AnalyticsRequest)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        var ENABLED: Boolean = true
    }
}
