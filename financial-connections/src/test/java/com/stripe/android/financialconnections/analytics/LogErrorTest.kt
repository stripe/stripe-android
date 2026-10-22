package com.stripe.android.financialconnections.analytics

import app.cash.turbine.Turbine
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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LogErrorTest {

    @Test
    fun `InstitutionUnplannedDowntimeError with live events in response should not emit live event`() = runScenario {
        val error = InstitutionUnplannedDowntimeError(
            institution = ApiKeyFixtures.institution(),
            showManualEntry = false,
            stripeException = APIException(
                stripeError = StripeError(
                    extraFields = mapOf(
                        "events_to_emit" to "[{\"type\":\"error\"}]"
                    )
                ),
            )
        )

        logError(error)

        assertThat(analyticsTracker.trackedEvents.awaitItem()).isEqualTo(
            FinancialConnectionsAnalyticsEvent.Error(
                extraMessage = "Test error",
                pane = Pane.PARTNER_AUTH,
                exception = error
            )
        )
        analyticsTracker.publicEvents.expectNoEvents()
    }

    @Test
    fun `browser unavailable should log expected analytics and emit browser unavailable event`() = runScenario {
        logError(
            AppInitializationError(
                message = "No Web browser available to launch AuthFlow",
                reason = AppInitializationError.Reason.BROWSER_UNAVAILABLE,
            )
        )

        val event = analyticsTracker.trackedEvents.awaitItem()
        assertThat(event.eventName).isEqualTo("linked_accounts.error.expected")
        assertThat(event.params?.get("error_type")).isEqualTo("AppInitializationError")
        assertPublicError(ErrorCode.WEB_BROWSER_UNAVAILABLE)
    }

    @Test
    fun `initialization error without browser reason remains unexpected even with browser message`() = runScenario {
        logError(AppInitializationError("No Web browser available to launch AuthFlow"))

        val event = analyticsTracker.trackedEvents.awaitItem()
        assertThat(event.eventName).isEqualTo("linked_accounts.error.unexpected")
        assertThat(event.params?.get("error_type")).isEqualTo("AppInitializationError")
        assertPublicError(ErrorCode.UNEXPECTED_ERROR)
    }

    @Test
    fun `generic exception should remain unexpected`() = runScenario {
        logError(IllegalStateException("Unexpected failure"))

        assertThat(analyticsTracker.trackedEvents.awaitItem().eventName)
            .isEqualTo("linked_accounts.error.unexpected")
        assertPublicError(ErrorCode.UNEXPECTED_ERROR)
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val analyticsTracker = FakeFinancialConnectionsAnalyticsTracker()
        Scenario(analyticsTracker).block()
        analyticsTracker.ensureAllEventsConsumed()
    }

    private class Scenario(val analyticsTracker: FakeFinancialConnectionsAnalyticsTracker) {
        fun logError(error: Throwable) {
            analyticsTracker.logError(
                extraMessage = "Test error",
                error = error,
                logger = Logger.noop(),
                pane = Pane.PARTNER_AUTH,
            )
        }

        suspend fun assertPublicError(errorCode: ErrorCode) {
            val event = analyticsTracker.publicEvents.awaitItem()
            assertThat(event.name).isEqualTo(Name.ERROR)
            assertThat(event.metadata?.errorCode).isEqualTo(errorCode)
        }
    }

    internal class FakeFinancialConnectionsAnalyticsTracker : FinancialConnectionsAnalyticsTracker {
        val trackedEvents = Turbine<FinancialConnectionsAnalyticsEvent>()
        val publicEvents = Turbine<PublicEvent>()

        override fun track(event: FinancialConnectionsAnalyticsEvent) {
            trackedEvents.add(event)
        }

        override fun emitEvent(name: Name, metadata: Metadata?) {
            publicEvents.add(PublicEvent(name, metadata))
        }

        fun ensureAllEventsConsumed() {
            trackedEvents.ensureAllEventsConsumed()
            publicEvents.ensureAllEventsConsumed()
        }

        data class PublicEvent(val name: Name, val metadata: Metadata?)
    }
}
