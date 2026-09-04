package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ImmediateVerticalPaymentSelectionHandlerTest {

    @Test
    fun `saved selection updates with user input before completion`() = runScenario {
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        handler.select(selection, true)

        assertThat(events.awaitItem()).isEqualTo(Event.SelectionUpdated(selection, true))
        assertThat(events.awaitItem()).isEqualTo(Event.SelectionCompleted)
    }

    @Test
    fun `Google Pay selection updates without user input before completion`() = runScenario {
        handler.select(PaymentSelection.GooglePay, false)

        assertThat(events.awaitItem()).isEqualTo(Event.SelectionUpdated(PaymentSelection.GooglePay, false))
        assertThat(events.awaitItem()).isEqualTo(Event.SelectionCompleted)
    }

    @Test
    fun `Link selection updates without user input before completion`() = runScenario {
        val selection = PaymentSelection.Link(brand = LinkBrand.Link)

        handler.select(selection, false)

        assertThat(events.awaitItem()).isEqualTo(Event.SelectionUpdated(selection, false))
        assertThat(events.awaitItem()).isEqualTo(Event.SelectionCompleted)
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val events = Turbine<Event>()
        val handler = ImmediateVerticalPaymentSelectionHandler(
            updateSelection = { selection, isUserInput ->
                events.add(Event.SelectionUpdated(selection, isUserInput))
            },
            onSelectionComplete = { events.add(Event.SelectionCompleted) },
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
