package com.stripe.android.networking

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class PaymentAnalyticsEventTest {

    @Test
    fun `event codes should be unique`() {
        val eventCodes = PaymentAnalyticsEvent.values().map { it.code }.distinct()
        assertThat(eventCodes)
            .hasSize(PaymentAnalyticsEvent.values().size)
    }
}
