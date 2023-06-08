package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.debug.DebugConfiguration
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.utils.Experiment
import com.stripe.android.financialconnections.utils.experimentAssignment
import com.stripe.android.financialconnections.utils.trackExposure
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
        debugConfiguration.overriddenNative?.let { return it }
        val killSwitchEnabled = nativeKillSwitchActive(manifest)
        val nativeExperimentEnabled = manifest
            .experimentAssignment(Experiment.CONNECTIONS_MOBILE_NATIVE) == EXPERIMENT_VALUE_NATIVE_TREATMENT
        return killSwitchEnabled.not() && nativeExperimentEnabled
    }

    @Suppress("ComplexCondition")
    suspend fun logExposure(manifest: FinancialConnectionsSessionManifest) {
        debugConfiguration.overriddenNative?.let { return }
        if (nativeKillSwitchActive(manifest).not()) {
            eventTracker.trackExposure(
                experiment = Experiment.CONNECTIONS_MOBILE_NATIVE,
                manifest = manifest
            )
        }
    }

    private fun nativeKillSwitchActive(manifest: FinancialConnectionsSessionManifest): Boolean =
        manifest.features
            ?.any { it.key == FEATURE_KEY_NATIVE_KILLSWITCH && it.value }
            ?: true

    companion object {
        private const val FEATURE_KEY_NATIVE_KILLSWITCH =
            "bank_connections_mobile_native_version_killswitch"
        private const val EXPERIMENT_VALUE_NATIVE_TREATMENT = "treatment"
    }
}
