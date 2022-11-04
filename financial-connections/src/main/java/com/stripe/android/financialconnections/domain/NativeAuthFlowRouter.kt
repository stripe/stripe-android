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

    private fun nativeAuthFlowKillSwitchActive(synchronizeSessionResponse: SynchronizeSessionResponse): Boolean {
        return synchronizeSessionResponse.manifest.features?.any { it.key == "bank_connections_mobile_native_version_killswitch" && it.value }
            ?: true
    }

    fun nativeAuthFlowEnabled(synchronizeSessionResponse: SynchronizeSessionResponse): Boolean {
        if (nativeAuthFlowKillSwitchActive(synchronizeSessionResponse)) return false
        // If experiments are not assigned, or native experiment is missing, fallback to webView.
        return synchronizeSessionResponse.manifest.experimentAssignments?.any { it.key == "connections_mobile_native" && it.value == "treatment" }
            ?: false
    }

     suspend fun logExposure(synchronizeSessionResponse: SynchronizeSessionResponse) {
        val killSwitchDisabled = !nativeAuthFlowKillSwitchActive(synchronizeSessionResponse)
        val experimentVariantIsPresent =
            synchronizeSessionResponse.manifest.experimentAssignments?.any { it.key == "connections_mobile_native" }
                ?: false
        val assignmentEventId = synchronizeSessionResponse.manifest.assignmentEventId
        val accountHolderId = synchronizeSessionResponse.manifest.accountholderToken
        if (killSwitchDisabled && experimentVariantIsPresent && assignmentEventId != null && accountHolderId != null) {
            eventTracker.track(
                FinancialConnectionsEvent.Exposure(
                    experimentName = "connections_mobile_native",
                    assignmentEventId = assignmentEventId,
                    accountHolderId = accountHolderId
                )
            )
        }
    }
}