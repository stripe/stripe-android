package com.stripe.android.financialconnections

import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent

internal class TestFinancialConnectionsAnalyticsTracker : FinancialConnectionsAnalyticsTracker {

    private val sentEvents = mutableListOf<FinancialConnectionsEvent>()

    override suspend fun track(event: FinancialConnectionsEvent): Result<Unit> {
        sentEvents += event
        return Result.success(Unit)
    }
}
