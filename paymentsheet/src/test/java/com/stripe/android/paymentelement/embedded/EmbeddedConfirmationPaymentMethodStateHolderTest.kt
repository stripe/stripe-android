@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedConfirmationStateHolder.Companion.CONFIRMATION_STATE_KEY
import com.stripe.android.paymentelement.embedded.EmbeddedConfirmationStateHolder.State
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class EmbeddedConfirmationPaymentMethodStateHolderTest {
    @Test
    fun `setting state updates savedStateHandle`() = testScenario {
        assertThat(savedStateHandle.get<State?>(CONFIRMATION_STATE_KEY)).isNull()
        val state = defaultState()
        confirmationStateHolder.state = state
        assertThat(savedStateHandle.get<State?>(CONFIRMATION_STATE_KEY)).isEqualTo(state)
    }

    @Test
    fun `initializing with state in savedStateHandle sets initial value`() {
        val state = defaultState()
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
        confirmationStateHolder.state = defaultState()
        assertThat(confirmationStateHolder.state?.selection?.paymentMethodType).isNull()
        selectionHolder.set(PaymentSelection.GooglePay)
        assertThat(confirmationStateHolder.state?.selection?.paymentMethodType).isEqualTo("google_pay")
    }

    private fun defaultState(): State = State(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        selection = null,
        initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(amount = 5000, currency = "USD"),
            )
        ),
        configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
    )

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
