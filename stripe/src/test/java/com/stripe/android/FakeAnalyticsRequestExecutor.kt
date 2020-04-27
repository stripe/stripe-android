package com.stripe.android

internal class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
    override fun executeAsync(request: AnalyticsRequest) {}
}
