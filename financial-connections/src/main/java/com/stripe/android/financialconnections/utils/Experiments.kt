package com.stripe.android.financialconnections.utils

import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Exposure
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest

internal fun FinancialConnectionsSessionManifest.experimentPresent(
    experiment: Experiment
): Boolean = experimentAssignment(experiment) != null

internal fun FinancialConnectionsSessionManifest.experimentAssignment(
    experiment: Experiment
): String? = experimentAssignments?.get(experiment.key)

internal suspend fun FinancialConnectionsAnalyticsTracker.trackExposure(
    experiment: Experiment,
    manifest: FinancialConnectionsSessionManifest
) {
    val assignmentEventId = manifest.assignmentEventId
    val accountHolderId = manifest.accountholderToken
    if (
        manifest.experimentPresent(experiment) &&
        assignmentEventId != null &&
        accountHolderId != null
    ) {
        track(
            Exposure(
                experimentName = experiment.key,
                assignmentEventId = assignmentEventId,
                accountHolderId = accountHolderId
            )
        )
    }
}

internal enum class Experiment(val key: String) {
    CONNECTIONS_CONSENT_COMBINED_LOGO("connections_consent_combined_logo"),
    CONNECTIONS_MOBILE_NATIVE("connections_mobile_native")
}
