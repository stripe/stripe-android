package com.stripe.android.financialconnections.utils

import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Exposure
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
internal class ExperimentsTest {

    private val tracker: FinancialConnectionsAnalyticsTracker = mock()

    @Test
    fun `trackExposure - track not called if experiment not present`() = runTest {
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            experimentAssignments = emptyMap(),
            assignmentEventId = "eventId",
            accountholderToken = "1234"
        )

        tracker.trackExposure(
            Experiment.CONNECTIONS_CONSENT_COMBINED_LOGO,
            manifest
        )

        verifyNoInteractions(tracker)
    }

    @Test
    fun `trackExposure - track called if experiment present`() = runTest {
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            experimentAssignments = mapOf(
                Experiment.CONNECTIONS_CONSENT_COMBINED_LOGO.key to "treatment"
            ),
            assignmentEventId = "eventId",
            accountholderToken = "1234"
        )

        tracker.trackExposure(
            Experiment.CONNECTIONS_CONSENT_COMBINED_LOGO,
            manifest
        )

        verify(tracker).track(
            Exposure(
                experimentName = Experiment.CONNECTIONS_CONSENT_COMBINED_LOGO.key,
                assignmentEventId = manifest.assignmentEventId!!,
                accountHolderId = manifest.accountholderToken!!
            )
        )
    }
}
