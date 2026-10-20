package com.stripe.android.financialconnections.analytics

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ErrorCode
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

internal class FinancialConnectionsResponseEventEmitterTest {

    @Test
    fun `emitIfPresent - institution_unavailable_planned error event`() = runScenario {
        emitter.emitIfPresent(errorResponse("institution_unavailable_planned"))

        assertErrorEvent(ErrorCode.INSTITUTION_UNAVAILABLE_PLANNED)
    }

    @Test
    fun `emitIfPresent - no_eligible_accounts error event`() = runScenario {
        emitter.emitIfPresent(errorResponse("no_eligible_accounts"))

        assertErrorEvent(ErrorCode.NO_ELIGIBLE_ACCOUNTS)
    }

    @Test
    fun `unknown backend error code remains an unexpected error`() = runScenario {
        emitter.emitIfPresent(errorResponse("new_backend_error"))

        assertErrorEvent(ErrorCode.UNEXPECTED_ERROR)
    }

    @Test
    fun `backend errors before synchronization do not emit an event`() = runScenario(initialManifest = null) {
        emitter.emitIfPresent(errorResponse("session_expired"))

        publicEvents.expectNoEvents()
        analyticsSender.calls.expectNoEvents()
    }

    @Test
    fun `malformed response events cannot interrupt request handling`() = runScenario {
        emitter.emitIfPresent(
            StripeResponse(code = 400, body = """{"error":{"extra_fields":{"events_to_emit":"invalid"}}}""")
        )

        publicEvents.expectNoEvents()
        analyticsSender.calls.expectNoEvents()
    }

    @Test
    fun `unsupported event types do not prevent later recognized events`() = runScenario {
        emitter.emitIfPresent(
            StripeResponse(
                code = 400,
                body = """
                    {"error":{"extra_fields":{"events_to_emit":[
                        {"type":"future_event"},
                        {"type":"error","error":{"error_code":"no_eligible_accounts"}}
                    ]}}}
                """.trimIndent()
            )
        )

        assertErrorEvent(ErrorCode.NO_ELIGIBLE_ACCOUNTS)
    }

    private fun errorResponse(errorCode: String) = StripeResponse(
        code = 400,
        body = """
            {
                "error": {
                    "extra_fields": {
                        "events_to_emit": [
                            {"type": "error", "error": {"error_code": "$errorCode"}}
                        ]
                    }
                }
            }
        """.trimIndent()
    )

    private fun runScenario(
        initialManifest: FinancialConnectionsSessionManifest? = ApiKeyFixtures.sessionManifest(),
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val publicEvents = Turbine<FinancialConnectionsEvent>()
        val analyticsSender = FakeFinancialConnectionsAnalyticsEventSender()
        val emitter = FinancialConnectionsResponseEventEmitter(
            json = Json.Default,
            logger = Logger.noop(),
            eventEmitter = FinancialConnectionsEventEmitter(
                eventContext = FinancialConnectionsEventContext(initialManifest),
                analyticsSender = analyticsSender,
                logger = Logger.noop(),
                workContext = backgroundScope.coroutineContext
            )
        )
        FinancialConnections.setEventListener(publicEvents::add)
        try {
            Scenario(emitter, analyticsSender, publicEvents).block()
        } finally {
            FinancialConnections.clearEventListener()
            analyticsSender.calls.ensureAllEventsConsumed()
            publicEvents.ensureAllEventsConsumed()
        }
    }

    private data class Scenario(
        val emitter: FinancialConnectionsResponseEventEmitter,
        val analyticsSender: FakeFinancialConnectionsAnalyticsEventSender,
        val publicEvents: Turbine<FinancialConnectionsEvent>
    ) {
        suspend fun assertErrorEvent(errorCode: ErrorCode) {
            val event = publicEvents.awaitItem()
            assertThat(event.name).isEqualTo(Name.ERROR)
            assertThat(event.metadata.errorCode).isEqualTo(errorCode)
            assertThat(event.financialConnectionsSessionId).isEqualTo(ApiKeyFixtures.sessionManifest().id)
            val analytics = analyticsSender.calls.awaitItem()
            assertThat(analytics.manifest.id).isEqualTo(event.financialConnectionsSessionId)
            assertThat(analytics.event.eventName).isEqualTo("linked_accounts.external_on_event.emitted")
        }
    }
}
