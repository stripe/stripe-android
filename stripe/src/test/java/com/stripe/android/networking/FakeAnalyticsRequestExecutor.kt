package com.stripe.android.networking

internal class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
    override fun executeAsync(request: AnalyticsRequest) {}
}
