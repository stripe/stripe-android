package com.stripe.android.financialconnections.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ErrorCode
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.exception.AppInitializationError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogErrorTest {

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
            verify(analyticsTracker, never()).emitEvent(any(), any())
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
        val event = argumentCaptor<FinancialConnectionsAnalyticsEvent>()
        verify(analyticsTracker).track(event.capture())
        assertThat(event.firstValue.eventName).isEqualTo("linked_accounts.error.expected")
        assertThat(event.firstValue.params?.get("error_type")).isEqualTo("AppInitializationError")

        // emits live event
        verify(analyticsTracker).emitEvent(
            name = Name.ERROR,
            metadata = Metadata(errorCode = ErrorCode.WEB_BROWSER_UNAVAILABLE)
        )
    }

    @Test
    fun `generic exception should remain unexpected`() = runTest {
        val analyticsTracker = mock<FinancialConnectionsAnalyticsTracker>()
        val error = IllegalStateException("Unexpected failure")

        analyticsTracker.logError(
            extraMessage = "Test error",
            error = error,
            logger = Logger.noop(),
            pane = Pane.PARTNER_AUTH,
        )

        val event = argumentCaptor<FinancialConnectionsAnalyticsEvent>()
        verify(analyticsTracker).track(event.capture())
        assertThat(event.firstValue.eventName).isEqualTo("linked_accounts.error.unexpected")
        verify(analyticsTracker).emitEvent(
            name = Name.ERROR,
            metadata = Metadata(errorCode = ErrorCode.UNEXPECTED_ERROR)
        )
    }
}
