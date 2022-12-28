package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.debug.DebugConfiguration
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import javax.inject.Inject

/**
 * Router class encapsulating logic of choosing Native vs Web AuthFlow.
 * Additionally, handles logging exposures when needed.
 */
internal class NativeAuthFlowRouter @Inject constructor(
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val debugConfiguration: DebugConfiguration
) {

    fun nativeAuthFlowEnabled(manifest: FinancialConnectionsSessionManifest): Boolean {
        debugConfiguration.overridenNative?.let { return it }
        val killSwitchEnabled = nativeKillSwitchActive(manifest)
        val nativeExperimentEnabled =
            manifest.experimentAssignment(EXPERIMENT_KEY_NATIVE) == EXPERIMENT_VALUE_NATIVE_TREATMENT
        return killSwitchEnabled.not() && nativeExperimentEnabled
    }

    @Suppress("ComplexCondition")
    suspend fun logExposure(manifest: FinancialConnectionsSessionManifest) {
        debugConfiguration.overridenNative?.let { return }
        val assignmentEventId = manifest.assignmentEventId
        val accountHolderId = manifest.accountholderToken
        val shouldLogExposure = nativeKillSwitchActive(manifest).not() &&
            manifest.experimentPresent(EXPERIMENT_KEY_NATIVE)
        if (
            shouldLogExposure &&
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

    private fun nativeKillSwitchActive(manifest: FinancialConnectionsSessionManifest): Boolean =
        manifest.features
            ?.any { it.key == FEATURE_KEY_NATIVE_KILLSWITCH && it.value }
            ?: true

    private fun FinancialConnectionsSessionManifest.experimentPresent(
        experimentKey: String
    ): Boolean = experimentAssignment(experimentKey) != null

    private fun FinancialConnectionsSessionManifest.experimentAssignment(
        experimentKey: String
    ): String? = experimentAssignments?.get(experimentKey)

    companion object {
        private const val FEATURE_KEY_NATIVE_KILLSWITCH =
            "bank_connections_mobile_native_version_killswitch"
        private const val EXPERIMENT_KEY_NATIVE = "connections_mobile_native"
        private const val EXPERIMENT_VALUE_NATIVE_TREATMENT = "treatment"
    }
}
