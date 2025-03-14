package com.stripe.android.connect.analytics

class FakeConnectAnalyticsService : ConnectAnalyticsService {
    override fun track(eventName: String, params: Map<String, Any?>) {
        // Nothing.
    }
}
