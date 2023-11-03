package com.stripe.android.financialconnections

import com.google.common.truth.Truth
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent
import com.google.common.truth.Truth.assertWithMessage
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker

internal class TestFinancialConnectionsAnalyticsTracker : FinancialConnectionsAnalyticsTracker {

    val sentEvents = mutableListOf<FinancialConnectionsAnalyticsEvent>()

    override suspend fun track(event: FinancialConnectionsAnalyticsEvent): Result<Unit> {
        sentEvents += event
        return Result.success(Unit)
    }

    /**
     * Asserts that an event with the given [expectedEventName] and **at least** the given
     * [expectedParams] has been tracked.
     *
     * use this when certain param properties are not relevant to the assertion.
     */
    fun assertContainsEvent(
        expectedEventName: String,
        expectedParams: Map<String, String>? = null
    ) {
        val containsEvent = sentEvents.any {
            it.eventName == expectedEventName &&
                expectedParams?.all { (k, v) -> it.params.orEmpty()[k] == v } == true
        }

        assertWithMessage(
            "Expected an event with name '$expectedEventName' " +
                "and params '$expectedParams', but no such event was found.\n" +
                "Emitted events: $sentEvents"
        ).that(containsEvent).isTrue()
    }
}
