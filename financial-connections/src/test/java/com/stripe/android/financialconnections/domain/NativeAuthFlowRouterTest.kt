package com.stripe.android.financialconnections.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Exposure
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.debug.DebugConfiguration
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

internal class NativeAuthFlowRouterTest {

    private val eventTracker = mock<FinancialConnectionsAnalyticsTracker>()
    private val debugConfiguration = mock<DebugConfiguration>()
    private val router = NativeAuthFlowRouter(
        eventTracker,
        debugConfiguration
    )

    init {
        whenever(debugConfiguration.overriddenNative).thenReturn(null)
    }

    @After
    fun after() {
        FeatureFlags.nativeInstantDebits.reset()
    }

    @Test
    fun `nativeAuthFlowEnabled - true if experiment is treatment and no kill switch`() {
        val syncResponse = syncWithAssignments(
            assignments = mapOf(
                "connections_mobile_native" to "treatment"
            ),
            features = mapOf()
        )
        val nativeAuthFlowEnabled = router.nativeAuthFlowEnabled(syncResponse.manifest, isInstantDebits = false)

        assertThat(nativeAuthFlowEnabled).isTrue()
    }

    @Test
    fun `nativeAuthFlowEnabled - false if native instant debits`() {
        FeatureFlags.nativeInstantDebits.setEnabled(true)

        val syncResponse = syncWithAssignments(
            assignments = mapOf(
                "connections_mobile_native" to "treatment"
            ),
            features = mapOf()
        )

        val nativeAuthFlowEnabled = router.nativeAuthFlowEnabled(syncResponse.manifest, isInstantDebits = true)
        assertThat(nativeAuthFlowEnabled).isTrue()
    }

    @Test
    fun `nativeAuthFlowEnabled - false if experiment is treatment but instant debits web`() {
        FeatureFlags.nativeInstantDebits.setEnabled(false)

        val syncResponse = syncWithAssignments(
            assignments = mapOf(
                "connections_mobile_native" to "treatment"
            ),
            features = mapOf()
        )
        val nativeAuthFlowEnabled = router.nativeAuthFlowEnabled(syncResponse.manifest, isInstantDebits = true)

        assertThat(nativeAuthFlowEnabled).isFalse()
    }

    @Test
    fun `nativeAuthFlowEnabled - false if experiment is treatment but kill switch is on`() {
        val syncResponse = syncWithAssignments(
            assignments = mapOf(
                "connections_mobile_native" to "treatment"
            ),
            features = mapOf(
                "bank_connections_mobile_native_version_killswitch" to true
            )
        )
        val nativeAuthFlowEnabled = router.nativeAuthFlowEnabled(syncResponse.manifest, isInstantDebits = false)

        assertThat(nativeAuthFlowEnabled).isFalse()
    }

    @Test
    fun `nativeAuthFlowEnabled - false if experiment is control`() {
        val syncResponse = syncWithAssignments(
            assignments = mapOf(
                "connections_mobile_native" to "control"
            ),
            features = mapOf(
                "bank_connections_mobile_native_version_killswitch" to false
            )
        )
        val nativeAuthFlowEnabled = router.nativeAuthFlowEnabled(syncResponse.manifest, isInstantDebits = false)

        assertThat(nativeAuthFlowEnabled).isFalse()
    }

    @Test
    fun `logExposure - logs if experiment present and kill switch is off`() = runTest {
        val syncResponse = syncWithAssignments(
            assignments = mapOf(
                "connections_mobile_native" to "random"
            ),
            features = mapOf(
                "bank_connections_mobile_native_version_killswitch" to false
            )
        )
        router.logExposure(syncResponse.manifest, isInstantDebits = false)

        verify(eventTracker).track(
            Exposure(
                experimentName = "connections_mobile_native",
                assignmentEventId = "id",
                accountHolderId = "token"
            )
        )
    }

    @Test
    fun `logExposure - does not log if instant debits and experiment present`() = runTest {
        val syncResponse = syncWithAssignments(
            assignments = mapOf(
                "connections_mobile_native" to "random"
            ),
            features = mapOf(
                "bank_connections_mobile_native_version_killswitch" to false
            )
        )
        router.logExposure(syncResponse.manifest, isInstantDebits = true)

        verifyNoInteractions(eventTracker)
    }

    @Test
    fun `logExposure - does not log if experiment present and kill switch is on`() = runTest {
        val syncResponse = syncWithAssignments(
            assignments = mapOf(
                "connections_mobile_native" to "random"
            ),
            features = mapOf(
                "bank_connections_mobile_native_version_killswitch" to true
            )
        )
        router.logExposure(syncResponse.manifest, isInstantDebits = false)

        verifyNoInteractions(eventTracker)
    }

    private fun syncWithAssignments(
        assignments: Map<String, String>,
        features: Map<String, Boolean>
    ) = ApiKeyFixtures.syncResponse().copy(
        manifest = ApiKeyFixtures.sessionManifest().copy(
            assignmentEventId = "id",
            accountholderToken = "token",
            experimentAssignments = assignments,
            features = features
        )
    )
}
