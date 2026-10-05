package com.stripe.android.financialconnections.analytics

import app.cash.turbine.Turbine
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest

internal class FakeFinancialConnectionsAnalyticsEventSender : FinancialConnectionsAnalyticsEventSender {
    var error: Throwable? = null
    val calls = Turbine<Call>()

    override suspend fun send(
        event: FinancialConnectionsAnalyticsEvent,
        manifest: FinancialConnectionsSessionManifest
    ) {
        calls.add(Call(event, manifest))
        error?.let { throw it }
    }

    data class Call(
        val event: FinancialConnectionsAnalyticsEvent,
        val manifest: FinancialConnectionsSessionManifest
    )
}
