package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ImmediateVerticalSavedPaymentMethodSelectionHandlerTest {

    @Test
    fun `selection is updated before completion`() = runTest {
        var updatedSelection: PaymentSelection.Saved? = null
        val completedSelections = Turbine<PaymentSelection.Saved?>()
        val handler = ImmediateVerticalSavedPaymentMethodSelectionHandler(
            updateSelection = { updatedSelection = it },
            onSelectionComplete = { completedSelections.add(updatedSelection) },
        )
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        assertThat(handler.state.value).isEqualTo(VerticalSavedPaymentMethodSelectionHandler.State.Idle)
        handler.select(selection)

        assertThat(completedSelections.awaitItem()).isEqualTo(selection)
        assertThat(updatedSelection).isEqualTo(selection)
        assertThat(handler.state.value).isEqualTo(VerticalSavedPaymentMethodSelectionHandler.State.Idle)
        completedSelections.ensureAllEventsConsumed()
    }
}
