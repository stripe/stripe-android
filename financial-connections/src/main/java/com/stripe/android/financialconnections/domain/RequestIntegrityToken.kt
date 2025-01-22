package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AttestationEndpoint
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AttestationRequestFailed
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AttestationRequestSucceeded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.attestation.IntegrityRequestManager
import jakarta.inject.Inject

internal class RequestIntegrityToken @Inject constructor(
    private val integrityRequestManager: IntegrityRequestManager,
    private val analyticsTracker: FinancialConnectionsAnalyticsTracker
) {
    suspend operator fun invoke(
        endpoint: AttestationEndpoint,
        pane: FinancialConnectionsSessionManifest.Pane
    ): String = integrityRequestManager.requestToken()
        .onSuccess {
            analyticsTracker.track(
                AttestationRequestSucceeded(
                    pane = pane,
                    endpoint = endpoint
                )
            )
        }
        .onFailure {
            analyticsTracker.track(
                AttestationRequestFailed(
                    pane = pane,
                    endpoint = endpoint,
                    error = it
                )
            )
        }
        .getOrThrow()
}
