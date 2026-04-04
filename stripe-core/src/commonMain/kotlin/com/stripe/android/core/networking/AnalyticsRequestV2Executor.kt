package com.stripe.android.core.networking

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface AnalyticsRequestV2Executor {
    suspend fun enqueue(request: AnalyticsRequestV2)
}
