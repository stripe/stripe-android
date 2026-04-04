package com.stripe.android.core.networking

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AnalyticsRequestV2Storage {
    suspend fun store(request: AnalyticsRequestV2): String
    suspend fun retrieve(id: String): AnalyticsRequestV2?
    suspend fun delete(id: String)
}
