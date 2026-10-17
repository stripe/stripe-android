package com.stripe.android.paymentelement.embedded

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder.Companion.EMBEDDED_PREVIOUS_SELECTIONS_KEY
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder.Companion.EMBEDDED_SELECTION_KEY
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder.Companion.EMBEDDED_TEMPORARY_SELECTION_KEY
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class DefaultEmbeddedSelectionHolderTest {
    @Test
    fun `setting selection emits value in selection state flow`() = testScenario {
        selectionHolder.selection.test {
            assertThat(awaitItem()).isNull()
            selectionHolder.setSelection(PaymentSelection.GooglePay)
            assertThat(awaitItem()?.paymentMethodType).isEqualTo("google_pay")
        }
    }

    @Test
    fun `setting selection updates savedStateHandle`() = testScenario {
        assertThat(savedStateHandle.get<PaymentSelection?>(EMBEDDED_SELECTION_KEY))
            .isNull()
        selectionHolder.setSelection(PaymentSelection.GooglePay)
        assertThat(savedStateHandle.get<PaymentSelection?>(EMBEDDED_SELECTION_KEY))
            .isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `setting selection updates previousNewSelections`() = testScenario {
        assertThat(selectionHolder.previousNewSelections.isEmpty).isTrue()
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        assertThat(selectionHolder.previousNewSelections.isEmpty).isFalse()
        assertThat(selectionHolder.previousNewSelections.size()).isEqualTo(1)
    }

    @Test
    fun `initializing with empty savedStateHandle stores previousNewSelections bundle`() = testScenario {
        val savedBundle = savedStateHandle.get<Bundle>(EMBEDDED_PREVIOUS_SELECTIONS_KEY)

        assertThat(savedBundle).isNotNull()
        assertThat(savedBundle?.isEmpty).isTrue()
    }

    @Test
    fun `setting new selection persists previousNewSelections in savedStateHandle`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

        val savedBundle = savedStateHandle.get<Bundle>(EMBEDDED_PREVIOUS_SELECTIONS_KEY)
        assertThat(savedBundle?.previousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

        val restoredHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        assertThat(restoredHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `initializing with selection in savedStateHandle sets initial value`() = testScenario(
        setup = {
            set(EMBEDDED_SELECTION_KEY, PaymentSelection.GooglePay)
        },
    ) {
        assertThat(savedStateHandle.get<PaymentSelection?>(EMBEDDED_SELECTION_KEY))
            .isEqualTo(PaymentSelection.GooglePay)
        selectionHolder.setSelection(null)
        assertThat(savedStateHandle.get<PaymentSelection?>(EMBEDDED_SELECTION_KEY))
            .isNull()
    }

    @Test
    fun `initializing with previousNewSelections in savedStateHandle sets initial value`() = testScenario(
        setup = {
            set(
                EMBEDDED_PREVIOUS_SELECTIONS_KEY,
                Bundle().apply {
                    putParcelable("cashapp", PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
                }
            )
        },
    ) {
        assertThat(selectionHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `setting temporarySelection emits value in temporarySelection state flow`() = testScenario {
        selectionHolder.temporarySelection.test {
            assertThat(awaitItem()).isNull()
            selectionHolder.setTemporarySelection("card")
            assertThat(awaitItem()).isEqualTo("card")
        }
    }

    @Test
    fun `setting temporarySelection updates savedStateHandle`() = testScenario {
        assertThat(savedStateHandle.get<PaymentMethodCode?>(EMBEDDED_TEMPORARY_SELECTION_KEY))
            .isNull()
        selectionHolder.setTemporarySelection("card")
        assertThat(savedStateHandle.get<PaymentMethodCode?>(EMBEDDED_TEMPORARY_SELECTION_KEY))
            .isEqualTo("card")
    }

    @Test
    fun `initializing with temporarySelection in savedStateHandle sets initial value`() = testScenario(
        setup = {
            set(EMBEDDED_TEMPORARY_SELECTION_KEY, "card")
        },
    ) {
        assertThat(savedStateHandle.get<PaymentMethodCode?>(EMBEDDED_TEMPORARY_SELECTION_KEY))
            .isEqualTo("card")
        selectionHolder.setTemporarySelection(null)
        assertThat(savedStateHandle.get<PaymentMethodCode?>(EMBEDDED_TEMPORARY_SELECTION_KEY))
            .isNull()
    }

    @Test
    fun `setting previousNewSelections updates previousNewSelections`() = testScenario {
        assertThat(selectionHolder.previousNewSelections.isEmpty).isTrue()
        val previousNewSelections = Bundle().apply {
            putParcelable("cashapp", PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        }
        selectionHolder.setPreviousNewSelections(previousNewSelections)
        assertThat(selectionHolder.previousNewSelections.isEmpty).isFalse()
        assertThat(selectionHolder.previousNewSelections.size()).isEqualTo(1)
    }

    @Test
    fun `setting previousNewSelections persists bundle in savedStateHandle`() = testScenario {
        val previousNewSelections = Bundle().apply {
            putParcelable("cashapp", PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        }

        selectionHolder.setPreviousNewSelections(previousNewSelections)

        val savedBundle = savedStateHandle.get<Bundle>(EMBEDDED_PREVIOUS_SELECTIONS_KEY)
        assertThat(savedBundle?.previousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
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
            selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle),
            savedStateHandle = savedStateHandle,
        ).block()
    }
}
