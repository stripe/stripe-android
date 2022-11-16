package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import javax.inject.Inject

/**
 * Router class encapsulating logic of choosing Native vs Web AuthFlow.
 * Additionally, handles logging exposures when needed.
 */
internal class NativeAuthFlowRouter @Inject constructor(
    val eventTracker: FinancialConnectionsAnalyticsTracker
) {

    fun nativeAuthFlowEnabled(sync: SynchronizeSessionResponse): Boolean {
        val killSwitchEnabled = nativeKillSwitchActive(sync)
        val nativeExperimentEnabled =
            sync.experimentAssignment(EXPERIMENT_KEY_NATIVE) == EXPERIMENT_VALUE_NATIVE_TREATMENT
        return killSwitchEnabled.not() && nativeExperimentEnabled
    }

    @Suppress("ComplexCondition")
    suspend fun logExposure(sync: SynchronizeSessionResponse) {
        val assignmentEventId = sync.manifest.assignmentEventId
        val accountHolderId = sync.manifest.accountholderToken
        if (
            nativeKillSwitchActive(sync).not() &&
            sync.experimentPresent(EXPERIMENT_KEY_NATIVE) &&
            assignmentEventId != null &&
            accountHolderId != null
        ) {
            eventTracker.track(
                FinancialConnectionsEvent.Exposure(
                    experimentName = EXPERIMENT_KEY_NATIVE,
                    assignmentEventId = assignmentEventId,
                    accountHolderId = accountHolderId
                )
            )
        }
    }

    private fun nativeKillSwitchActive(sync: SynchronizeSessionResponse): Boolean =
        sync.manifest.features
            ?.any { it.key == FEATURE_KEY_NATIVE_KILLSWITCH && it.value }
            ?: true

    private fun SynchronizeSessionResponse.experimentPresent(
        experimentKey: String
    ): Boolean = experimentAssignment(experimentKey) != null

    private fun SynchronizeSessionResponse.experimentAssignment(
        experimentKey: String
    ): String? = manifest.experimentAssignments?.get(experimentKey)

    companion object {
        private const val FEATURE_KEY_NATIVE_KILLSWITCH =
            "bank_connections_mobile_native_version_killswitch"
        private const val EXPERIMENT_KEY_NATIVE = "connections_mobile_native"
        private const val EXPERIMENT_VALUE_NATIVE_TREATMENT = "treatment"
    }
}
