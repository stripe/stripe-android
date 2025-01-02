package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AttestationRequestFailed
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AttestationRequestSucceeded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.attestation.IntegrityRequestManager

internal class RequestIntegrityToken(
    private val integrityRequestManager: IntegrityRequestManager,
    private val analyticsTracker: FinancialConnectionsAnalyticsTracker
) {
    suspend operator fun invoke(): String = integrityRequestManager.requestToken()
        .onSuccess {
            analyticsTracker.track(
                AttestationRequestSucceeded()
            )
        }
        .onFailure {
            analyticsTracker.track(
                AttestationRequestFailed(it.message ?: "Unknown error")
            )
        }
        .getOrThrow()
}