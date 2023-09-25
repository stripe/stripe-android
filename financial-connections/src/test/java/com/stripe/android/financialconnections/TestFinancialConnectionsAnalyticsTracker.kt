package com.stripe.android.financialconnections

import com.google.common.truth.Truth
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent
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
        Truth.assertThat(
            sentEvents.any {
                it.eventName == expectedEventName &&
                    expectedParams
                        .orEmpty()
                        .all { (k, v) -> it.params.orEmpty()[k] == v }
            }
        ).isTrue()
    }
}
