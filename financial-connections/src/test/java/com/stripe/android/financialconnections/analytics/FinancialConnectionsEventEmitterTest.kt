package com.stripe.android.financialconnections.analytics

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ErrorCode
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class FinancialConnectionsEventEmitterTest {

    @Test
    fun `events before the canonical session is known are suppressed`() = runScenario(initialManifest = null) {
        emitter.emit(Name.ERROR, Metadata(errorCode = ErrorCode.UNEXPECTED_ERROR))

        publicEvents.expectNoEvents()
        analyticsSender.calls.expectNoEvents()
    }

    @Test
    fun `blank session ID is never exposed`() = runScenario(
        initialManifest = ApiKeyFixtures.sessionManifest().copy(id = "")
    ) {
        emitter.emit(Name.OPEN, Metadata())

        publicEvents.expectNoEvents()
        analyticsSender.calls.expectNoEvents()
    }

    @Test
    fun `open uses the canonical ID without replaying earlier errors`() = runScenario(initialManifest = null) {
        emitter.emit(Name.ERROR, Metadata(errorCode = ErrorCode.UNEXPECTED_ERROR))
        eventContext.update(ApiKeyFixtures.sessionManifest().copy(id = "fcsess_canonical"))

        emitter.emit(Name.OPEN, Metadata())

        val event = awaitEvent(Name.OPEN)
        assertThat(event.financialConnectionsSessionId).isEqualTo("fcsess_canonical")
    }

    @Test
    fun `new presentation cannot reuse the previous presentation ID`() = runScenario {
        emitter.emit(Name.CANCEL, Metadata())
        val original = awaitEvent(Name.CANCEL)
        val nextContext = FinancialConnectionsEventContext(null)
        val nextEmitter = createEmitter(nextContext)

        nextEmitter.emit(Name.ERROR, Metadata(errorCode = ErrorCode.UNEXPECTED_ERROR))
        publicEvents.expectNoEvents()
        analyticsSender.calls.expectNoEvents()

        nextContext.update(ApiKeyFixtures.sessionManifest().copy(id = "fcsess_next"))
        nextEmitter.emit(Name.OPEN, Metadata())
        assertThat(awaitEvent(Name.OPEN).financialConnectionsSessionId).isEqualTo("fcsess_next")

        emitter.emit(Name.SUCCESS, Metadata())
        assertThat(awaitEvent(Name.SUCCESS).financialConnectionsSessionId)
            .isEqualTo(original.financialConnectionsSessionId)
    }

    @Test
    fun `browser flow events retain the canonical session ID`() = runScenario {
        emitter.emit(Name.OPEN, Metadata())
        val opened = awaitEvent(Name.OPEN)
        emitter.emit(Name.FLOW_LAUNCHED_IN_BROWSER, Metadata())
        assertThat(awaitEvent(Name.FLOW_LAUNCHED_IN_BROWSER).financialConnectionsSessionId)
            .isEqualTo(opened.financialConnectionsSessionId)
        emitter.emit(Name.CANCEL, Metadata())
        assertThat(awaitEvent(Name.CANCEL).financialConnectionsSessionId)
            .isEqualTo(opened.financialConnectionsSessionId)
    }

    @Test
    fun `analytics contains the same public event payload and session`() = runScenario {
        val metadata = Metadata(
            institutionName = "Test Bank",
            manualEntry = false,
            errorCode = ErrorCode.NO_ELIGIBLE_ACCOUNTS
        )

        emitter.emit(Name.ERROR, metadata)

        val event = publicEvents.awaitItem()
        assertThat(event.metadata).isEqualTo(metadata)
        val call = analyticsSender.calls.awaitItem()
        assertThat(call.event.eventName).isEqualTo("linked_accounts.external_on_event.emitted")
        assertThat(call.manifest.id).isEqualTo(event.financialConnectionsSessionId)
        val payload = Json.parseToJsonElement(requireNotNull(call.event.params?.get("event_payload"))).jsonObject
        assertThat(payload.getValue("name").jsonPrimitive.content).isEqualTo("error")
        assertThat(payload.getValue("financialConnectionsSessionId").jsonPrimitive.content)
            .isEqualTo(event.financialConnectionsSessionId)
        assertThat(payload.getValue("metadata")).isEqualTo(
            Json.parseToJsonElement(
                """{"institutionName":"Test Bank","manualEntry":false,"errorCode":"no_eligible_accounts"}"""
            )
        )
    }

    @Test
    fun `analytics uses the manifest captured when the event was emitted`() = runScenario {
        val originalId = requireNotNull(eventContext.manifest).id
        emitter.emit(Name.OPEN, Metadata())
        eventContext.update(ApiKeyFixtures.sessionManifest().copy(id = "fcsess_changed"))

        assertThat(awaitEvent(Name.OPEN).financialConnectionsSessionId).isEqualTo(originalId)
    }

    @Test
    fun `listener failure does not interrupt the flow or analytics`() = runScenario(listenerThrows = true) {
        emitter.emit(Name.OPEN, Metadata())

        awaitEvent(Name.OPEN)
    }

    @Test
    fun `analytics failure cannot recursively emit a public error`() = runScenario {
        analyticsSender.error = IllegalStateException("Telemetry unavailable")

        emitter.emit(Name.SUCCESS, Metadata())

        awaitEvent(Name.SUCCESS)
        publicEvents.expectNoEvents()
        analyticsSender.calls.expectNoEvents()
    }

    private fun runScenario(
        initialManifest: FinancialConnectionsSessionManifest? = ApiKeyFixtures.sessionManifest(),
        listenerThrows: Boolean = false,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val publicEvents = Turbine<FinancialConnectionsEvent>()
        val analyticsSender = FakeFinancialConnectionsAnalyticsEventSender()
        val eventContext = FinancialConnectionsEventContext(initialManifest)
        val createEmitter = { context: FinancialConnectionsEventContext ->
            FinancialConnectionsEventEmitter(
                eventContext = context,
                analyticsSender = analyticsSender,
                logger = Logger.noop(),
                workContext = backgroundScope.coroutineContext
            )
        }
        FinancialConnections.setEventListener {
            publicEvents.add(it)
            if (listenerThrows) error("Merchant listener failed")
        }
        try {
            Scenario(createEmitter(eventContext), eventContext, analyticsSender, publicEvents, createEmitter).block()
            testScheduler.runCurrent()
        } finally {
            FinancialConnections.clearEventListener()
            analyticsSender.calls.ensureAllEventsConsumed()
            publicEvents.ensureAllEventsConsumed()
        }
    }

    private data class Scenario(
        val emitter: FinancialConnectionsEventEmitter,
        val eventContext: FinancialConnectionsEventContext,
        val analyticsSender: FakeFinancialConnectionsAnalyticsEventSender,
        val publicEvents: Turbine<FinancialConnectionsEvent>,
        val createEmitter: (FinancialConnectionsEventContext) -> FinancialConnectionsEventEmitter
    ) {
        suspend fun awaitEvent(name: Name): FinancialConnectionsEvent {
            val event = publicEvents.awaitItem()
            assertThat(event.name).isEqualTo(name)
            val call = analyticsSender.calls.awaitItem()
            assertThat(call.event.eventName).isEqualTo("linked_accounts.external_on_event.emitted")
            assertThat(call.manifest.id).isEqualTo(event.financialConnectionsSessionId)
            return event
        }
    }
}
