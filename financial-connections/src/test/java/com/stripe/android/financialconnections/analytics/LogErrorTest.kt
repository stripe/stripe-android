package com.stripe.android.financialconnections.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ErrorCode
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogErrorTest {

    private val liveEvents = mutableListOf<FinancialConnectionsEvent>()

    @Before
    fun setUp() {
        FinancialConnections.setEventListener { liveEvents += it }
    }

    @Test
    fun `InstitutionUnplannedDowntimeError should log analytics and emit live event`() = runTest {
        // Given
        val analyticsTracker = mock<FinancialConnectionsAnalyticsTracker>()

        val unplannedDowntimeError = InstitutionUnplannedDowntimeError(
            institution = ApiKeyFixtures.institution(),
            showManualEntry = false,
            stripeException = APIException()
        )

        // When
        analyticsTracker.logError(
            extraMessage = "Test error",
            error = unplannedDowntimeError,
            logger = Logger.noop(),
            pane = FinancialConnectionsSessionManifest.Pane.PARTNER_AUTH,
        )

        // Then
        // logs analytics
        verify(analyticsTracker).track(
            FinancialConnectionsAnalyticsEvent.Error(
                extraMessage = "Test error",
                pane = FinancialConnectionsSessionManifest.Pane.PARTNER_AUTH,
                exception = unplannedDowntimeError
            )
        )

        // emits live event
        assertThat(liveEvents).containsExactly(
            FinancialConnectionsEvent(
                name = Name.ERROR,
                metadata = Metadata(
                    errorCode = ErrorCode.INSTITUTION_UNAVAILABLE_UNPLANNED
                )
            )
        )
    }

    @After
    fun tearDown() {
        liveEvents.clear()
    }
}
