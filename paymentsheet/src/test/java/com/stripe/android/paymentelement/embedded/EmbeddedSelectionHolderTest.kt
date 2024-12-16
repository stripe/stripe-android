package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class EmbeddedSelectionHolderTest {
    @Test
    fun `setting selection emits value in selection state flow`() = testScenario {
        selectionHolder.selection.test {
            assertThat(awaitItem()).isNull()
            selectionHolder.set(PaymentSelection.GooglePay)
            assertThat(awaitItem()?.paymentMethodType).isEqualTo("google_pay")
        }
    }

    @Test
    fun `setting selection updates savedStateHandle`() = testScenario {
        assertThat(savedStateHandle.get<PaymentSelection?>(EmbeddedSelectionHolder.EMBEDDED_SELECTION_KEY))
            .isNull()
        selectionHolder.set(PaymentSelection.GooglePay)
        assertThat(savedStateHandle.get<PaymentSelection?>(EmbeddedSelectionHolder.EMBEDDED_SELECTION_KEY))
            .isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `initializing with selection in savedStateHandle sets initial value`() = testScenario(
        setup = {
            set(EmbeddedSelectionHolder.EMBEDDED_SELECTION_KEY, PaymentSelection.GooglePay)
        },
    ) {
        assertThat(savedStateHandle.get<PaymentSelection?>(EmbeddedSelectionHolder.EMBEDDED_SELECTION_KEY))
            .isEqualTo(PaymentSelection.GooglePay)
        selectionHolder.set(null)
        assertThat(savedStateHandle.get<PaymentSelection?>(EmbeddedSelectionHolder.EMBEDDED_SELECTION_KEY))
            .isNull()
    }

    private class Scenario(
        val selectionHolder: EmbeddedSelectionHolder,
        val savedStateHandle: SavedStateHandle,
    )

    private fun testScenario(
        setup: SavedStateHandle.() -> Unit = {},
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        setup(savedStateHandle)
        Scenario(
            selectionHolder = EmbeddedSelectionHolder(savedStateHandle),
            savedStateHandle = savedStateHandle,
        ).block()
    }
}
