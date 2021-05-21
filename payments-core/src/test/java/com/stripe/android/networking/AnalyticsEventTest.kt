package com.stripe.android.networking

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class AnalyticsEventTest {

    @Test
    fun `event codes should be unique`() {
        val eventCodes = AnalyticsEvent.values().map { it.code }.distinct()
        assertThat(eventCodes)
            .hasSize(AnalyticsEvent.values().size)
    }
}
