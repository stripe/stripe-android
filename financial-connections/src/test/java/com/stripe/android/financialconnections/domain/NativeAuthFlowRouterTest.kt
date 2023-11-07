package com.stripe.android.financialconnections.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Exposure
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.debug.DebugConfiguration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun `nativeAuthFlowEnabled - true if experiment is treatment and no kill switch`() {
        val syncResponse = syncWithAssignments(
            assignments = mapOf(
                "connections_mobile_native" to "treatment"
            ),
            features = mapOf()
        )
        val nativeAuthFlowEnabled = router.nativeAuthFlowEnabled(syncResponse.manifest)

        assertThat(nativeAuthFlowEnabled).isTrue()
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
        val nativeAuthFlowEnabled = router.nativeAuthFlowEnabled(syncResponse.manifest)

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
        val nativeAuthFlowEnabled = router.nativeAuthFlowEnabled(syncResponse.manifest)

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
        router.logExposure(syncResponse.manifest)

        verify(eventTracker).track(
            Exposure(
                experimentName = "connections_mobile_native",
                assignmentEventId = "id",
                accountHolderId = "token"
            )
        )
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
        router.logExposure(syncResponse.manifest)

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
