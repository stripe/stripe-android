package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsEvent
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsService

internal class TestOnrampAnalyticsService : OnrampAnalyticsService {

    override val elementsSessionId: String = "test-session-id"

    private val sentEvents = mutableListOf<OnrampAnalyticsEvent>()

    override fun track(event: OnrampAnalyticsEvent) {
        sentEvents += event
    }

    fun assertContainsEvent(expectedEvent: OnrampAnalyticsEvent) {
        Truth.assertThat(
            sentEvents.any {
                it.eventName == expectedEvent.eventName &&
                    it.params == expectedEvent.params
            }
        ).isTrue()
    }
}
