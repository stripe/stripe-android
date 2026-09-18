package com.stripe.android.financialconnections

import app.cash.turbine.Turbine
import com.google.common.truth.Truth
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name

internal class TestFinancialConnectionsAnalyticsTracker : FinancialConnectionsAnalyticsTracker {

    val sentEvents = mutableListOf<FinancialConnectionsAnalyticsEvent>()
    val publicEvents = Turbine<PublicEvent>()

    override fun track(event: FinancialConnectionsAnalyticsEvent) {
        sentEvents += event
    }

    override fun emitEvent(name: Name, metadata: Metadata) {
        publicEvents.add(PublicEvent(name, metadata))
    }

    data class PublicEvent(val name: Name, val metadata: Metadata)

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
