package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ImmediateVerticalPaymentSelectionHandlerTest {

    @Test
    fun `updates selection before invoking supplied completion`() = runScenario(
        completion = { events -> events.add(Event.SelectionCompleted) },
    ) {
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        assertThat(handler.state.value).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)
        handler.select(selection, true)

        assertThat(events.awaitItem()).isEqualTo(Event.SelectionUpdated(selection, true))
        assertThat(events.awaitItem()).isEqualTo(Event.SelectionCompleted)
    }

    @Test
    fun `invokes supplied completion without updating selection`() = runScenario(
        completion = { events -> events.add(Event.SelectionCompleted) },
    ) {
        handler.onSelectionComplete()

        assertThat(events.awaitItem()).isEqualTo(Event.SelectionCompleted)
        events.expectNoEvents()
    }

    @Test
    fun `clearFailure is a no-op`() = runScenario(completion = null) {
        handler.clearFailure()

        assertThat(handler.state.value).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)
    }

    @Test
    fun `updates selection when completion action is absent`() = runScenario(
        completion = null,
    ) {
        handler.select(PaymentSelection.GooglePay, false)

        assertThat(events.awaitItem()).isEqualTo(Event.SelectionUpdated(PaymentSelection.GooglePay, false))
        handler.onSelectionComplete()
        events.expectNoEvents()
    }

    private fun runScenario(
        completion: ((Turbine<Event>) -> Unit)?,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val events = Turbine<Event>()
        val handler = ImmediateVerticalPaymentSelectionHandler(
            updateSelection = { selection, isUserInput ->
                events.add(Event.SelectionUpdated(selection, isUserInput))
            },
            completionAction = completion?.let { completion -> { completion(events) } },
        )

        Scenario(
            handler = handler,
            events = events,
        ).apply { block() }

        events.ensureAllEventsConsumed()
    }

    private class Scenario(
        val handler: VerticalPaymentSelectionHandler,
        val events: ReceiveTurbine<Event>,
    )

    private sealed interface Event {
        data class SelectionUpdated(
            val selection: PaymentSelection,
            val isUserInput: Boolean,
        ) : Event

        data object SelectionCompleted : Event
    }
}
