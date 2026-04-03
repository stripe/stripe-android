package com.stripe.android.core.utils

import com.stripe.android.core.networking.AnalyticsRequestV2
import com.stripe.android.core.networking.AnalyticsRequestV2Storage
import java.util.UUID

internal class FakeAnalyticsRequestV2Storage : AnalyticsRequestV2Storage {

    private val store = mutableMapOf<String, AnalyticsRequestV2>()

    override suspend fun store(request: AnalyticsRequestV2): String {
        val id = UUID.randomUUID().toString()
        store[id] = request
        return id
    }

    override suspend fun retrieve(id: String): AnalyticsRequestV2? {
        return store[id]
    }

    override suspend fun delete(id: String) {
        store.remove(id)
    }
}
