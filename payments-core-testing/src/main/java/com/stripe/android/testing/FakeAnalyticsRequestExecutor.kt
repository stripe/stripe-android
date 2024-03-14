package com.stripe.android.testing

import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor

class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {

    private val executedRequests = mutableListOf<AnalyticsRequest>()
    override fun executeAsync(request: AnalyticsRequest) {
        executedRequests.add(request)
    }

    fun getExecutedRequests(): List<AnalyticsRequest> {
        return executedRequests.toList()
    }

    fun clear() {
        executedRequests.clear()
    }
}
