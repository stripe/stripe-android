package com.stripe.android.financialconnections.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ErrorCode
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.exception.AppInitializationError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
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
    fun `InstitutionUnplannedDowntimeError with live events in response should not emit live event`() =
        runTest {
            // Given
            val analyticsTracker = mock<FinancialConnectionsAnalyticsTracker>()

            val unplannedDowntimeError = InstitutionUnplannedDowntimeError(
                institution = ApiKeyFixtures.institution(),
                showManualEntry = false,
                // Simulates an api error that includes events to emit.
                stripeException = APIException(
                    stripeError = StripeError(
                        extraFields = mapOf(
                            "events_to_emit" to "[{\"type\":\"error\"}]"
                        )
                    ),
                )
            )

            // When
            analyticsTracker.logError(
                extraMessage = "Test error",
                error = unplannedDowntimeError,
                logger = Logger.noop(),
                pane = Pane.PARTNER_AUTH,
            )

            // Then
            // logs analytics
            verify(analyticsTracker).track(
                FinancialConnectionsAnalyticsEvent.Error(
                    extraMessage = "Test error",
                    pane = Pane.PARTNER_AUTH,
                    exception = unplannedDowntimeError
                )
            )

            // emits live event
            assertThat(liveEvents).isEmpty()
        }

    @Test
    fun `AppInitializationError should log analytics and emit live event`() = runTest {
        // Given
        val analyticsTracker = mock<FinancialConnectionsAnalyticsTracker>()

        val appInitializationError = AppInitializationError(
            message = "Browser unavailable",
        )

        // When
        analyticsTracker.logError(
            extraMessage = "Test error",
            error = appInitializationError,
            logger = Logger.noop(),
            pane = Pane.PARTNER_AUTH,
        )

        // Then
        // logs analytics
        verify(analyticsTracker).track(
            FinancialConnectionsAnalyticsEvent.Error(
                extraMessage = "Test error",
                pane = Pane.PARTNER_AUTH,
                exception = appInitializationError
            )
        )

        // emits live event
        assertThat(liveEvents).contains(
            FinancialConnectionsEvent(
                name = Name.ERROR,
                metadata = Metadata(
                    errorCode = ErrorCode.WEB_BROWSER_UNAVAILABLE
                )
            )
        )
    }

    @After
    fun tearDown() {
        liveEvents.clear()
    }
}
