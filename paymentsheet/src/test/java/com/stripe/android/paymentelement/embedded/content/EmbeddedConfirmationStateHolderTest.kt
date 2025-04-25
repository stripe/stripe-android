@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder.Companion.CONFIRMATION_STATE_KEY
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder.State
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class EmbeddedConfirmationStateHolderTest {
    @Test
    fun `setting state updates savedStateHandle`() = testScenario {
        assertThat(savedStateHandle.get<State?>(CONFIRMATION_STATE_KEY)).isNull()
        val state = EmbeddedConfirmationStateFixtures.defaultState()
        confirmationStateHolder.state = state
        assertThat(savedStateHandle.get<State?>(CONFIRMATION_STATE_KEY)).isEqualTo(state)
    }

    @Test
    fun `initializing with state in savedStateHandle sets initial value`() {
        val state = EmbeddedConfirmationStateFixtures.defaultState()
        testScenario(
            setup = {
                set(CONFIRMATION_STATE_KEY, state)
            },
        ) {
            assertThat(savedStateHandle.get<State?>(CONFIRMATION_STATE_KEY)).isEqualTo(state)
            confirmationStateHolder.state = null
            assertThat(savedStateHandle.get<State?>(CONFIRMATION_STATE_KEY)).isNull()
        }
    }

    @Test
    fun `updating selection updates state with selection`() = testScenario {
        confirmationStateHolder.state = EmbeddedConfirmationStateFixtures.defaultState()
        assertThat(confirmationStateHolder.state?.selection?.paymentMethodType).isNull()
        selectionHolder.set(PaymentSelection.GooglePay)
        assertThat(confirmationStateHolder.state?.selection?.paymentMethodType).isEqualTo("google_pay")
    }

    @Test
    fun `updating state or selection updates stateFlow`() = testScenario {
        confirmationStateHolder.stateFlow.test {
            assertThat(awaitItem()).isNull()
            confirmationStateHolder.state = EmbeddedConfirmationStateFixtures.defaultState()
            awaitItem().let {
                assertThat(it).isNotNull()
                assertThat(it?.selection?.paymentMethodType).isNull()
            }
            selectionHolder.set(PaymentSelection.GooglePay)
            assertThat(awaitItem()?.selection?.paymentMethodType).isEqualTo("google_pay")
        }
    }

    private class Scenario(
        val selectionHolder: EmbeddedSelectionHolder,
        val confirmationStateHolder: EmbeddedConfirmationStateHolder,
        val savedStateHandle: SavedStateHandle,
    )

    private fun testScenario(
        setup: SavedStateHandle.() -> Unit = {},
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        setup(savedStateHandle)
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        val confirmationStateHolder = EmbeddedConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        )
        Scenario(
            selectionHolder = selectionHolder,
            confirmationStateHolder = confirmationStateHolder,
            savedStateHandle = savedStateHandle,
        ).block()
    }
}
