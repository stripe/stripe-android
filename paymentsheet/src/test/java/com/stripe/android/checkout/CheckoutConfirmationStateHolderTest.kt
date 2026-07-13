package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class CheckoutConfirmationStateHolderTest {
    @Test
    fun `selection holder changes update confirmation state selection`() = runScenario {
        confirmationStateHolder.state = state(selection = PaymentSelection.GooglePay)

        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        assertThat(confirmationStateHolder.state?.selection)
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `selection holder changes are a no-op while state is null`() = runScenario {
        // Before configure commits a state, the holder is null; a selection change must not
        // materialize a state (nor throw), since state?.copy is a no-op on null.
        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        assertThat(confirmationStateHolder.state).isNull()
    }

    @Test
    fun `state round-trips through the saved state handle`() = runScenario {
        // Align the selection holder with the state so the recreated holder's init collector, which
        // immediately re-emits the current selection, copies the same value and leaves state intact.
        selectionHolder.set(PaymentSelection.GooglePay)
        val state = state(selection = PaymentSelection.GooglePay)
        confirmationStateHolder.state = state

        // A fresh holder over the same handle sees the persisted state (survives process death).
        val recreated = CheckoutConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        )
        assertThat(recreated.state).isEqualTo(state)
    }

    @Test
    fun `stateFlow emits the initial null and each committed state`() = runScenario {
        confirmationStateHolder.stateFlow.test {
            assertThat(awaitItem()).isNull()

            val committed = state(selection = PaymentSelection.GooglePay)
            confirmationStateHolder.state = committed
            assertThat(awaitItem()).isEqualTo(committed)
        }
    }

    @Test
    fun `stateFlow emits the updated selection when the selection holder changes`() = runScenario {
        confirmationStateHolder.state = state(selection = PaymentSelection.GooglePay)

        confirmationStateHolder.stateFlow.test {
            assertThat(awaitItem()?.selection).isEqualTo(PaymentSelection.GooglePay)

            selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            assertThat(awaitItem()?.selection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        }
    }

    private fun state(selection: PaymentSelection?) = CheckoutConfirmationStateHolder.State(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        selection = selection,
        configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
    )

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        val confirmationStateHolder = CheckoutConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        )

        Scenario(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            confirmationStateHolder = confirmationStateHolder,
        ).block()
    }

    private class Scenario(
        val savedStateHandle: SavedStateHandle,
        val selectionHolder: EmbeddedSelectionHolder,
        val confirmationStateHolder: CheckoutConfirmationStateHolder,
    )
}
