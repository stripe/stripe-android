package com.stripe.android

import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsEventTest {

    @Test
    fun testUniqueCodes() {
        val actual = AnalyticsEvent.values().map { it.code }.toSet().size
        val expected = AnalyticsEvent.values().size

        assertEquals(expected, actual)
    }
}
