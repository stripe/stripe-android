package com.stripe.android.paymentelement.embedded

import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder.Companion.EMBEDDED_PREVIOUS_SELECTIONS_KEY
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder.Companion.EMBEDDED_SELECTION_KEY
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder.Companion.EMBEDDED_TEMPORARY_SELECTION_KEY
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
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
        assertThat(selectionHolder.previousNewSelections["cashapp"])
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `initializing with empty savedStateHandle stores previousNewSelections`() = testScenario {
        val savedSelections = savedStateHandle.get<PreviousNewSelections>(EMBEDDED_PREVIOUS_SELECTIONS_KEY)

        assertThat(savedSelections).isNotNull()
        assertThat(savedSelections?.isEmpty).isTrue()
    }

    @Test
    fun `setting new selection persists previousNewSelections in savedStateHandle`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

        val savedSelections = savedStateHandle.get<PreviousNewSelections>(EMBEDDED_PREVIOUS_SELECTIONS_KEY)
        assertThat(savedSelections?.get("cashapp"))
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
                PreviousNewSelections.empty
                    .updatedWith(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
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
        val previousNewSelections = PreviousNewSelections.empty
            .updatedWith(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        selectionHolder.setPreviousNewSelections(previousNewSelections)
        assertThat(selectionHolder.previousNewSelections.isEmpty).isFalse()
        assertThat(selectionHolder.previousNewSelections["cashapp"])
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `setting previousNewSelections persists in savedStateHandle`() = testScenario {
        val previousNewSelections = PreviousNewSelections.empty
            .updatedWith(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

        selectionHolder.setPreviousNewSelections(previousNewSelections)

        val savedSelections = savedStateHandle.get<PreviousNewSelections>(EMBEDDED_PREVIOUS_SELECTIONS_KEY)
        assertThat(savedSelections?.get("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `new selection replaces only the entry for its payment method code`() = testScenario {
        val original = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val replacement = original.copy(brand = CardBrand.MasterCard)
        selectionHolder.setSelection(original)
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        val previousHistory = selectionHolder.previousNewSelections

        selectionHolder.setSelection(replacement)

        assertThat(selectionHolder.getPreviousNewSelection("card")).isEqualTo(replacement)
        assertThat(selectionHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        assertThat(previousHistory["card"]).isEqualTo(original)
    }

    @Test
    fun `incoming history wins when merging entries for the same payment method`() = testScenario {
        val original = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val replacement = original.copy(brand = CardBrand.MasterCard)
        selectionHolder.setSelection(original)
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

        selectionHolder.setPreviousNewSelections(PreviousNewSelections.empty.updatedWith(replacement))

        assertThat(selectionHolder.getPreviousNewSelection("card")).isEqualTo(replacement)
        assertThat(selectionHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `null selection preserves history`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

        selectionHolder.setSelection(null)

        assertThat(selectionHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `non-new selection preserves history`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

        selectionHolder.setSelection(PaymentSelection.GooglePay)

        assertThat(selectionHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `clearing history persists an empty collection`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        assertThat(selectionHolder.previousNewSelections.isEmpty).isFalse()

        selectionHolder.clearPreviousNewSelections()

        assertThat(selectionHolder.previousNewSelections.isEmpty).isTrue()
        assertThat(savedStateHandle.get<PreviousNewSelections>(EMBEDDED_PREVIOUS_SELECTIONS_KEY))
            .isEqualTo(PreviousNewSelections.empty)
        assertThat(DefaultEmbeddedSelectionHolder(restoreSavedState(savedStateHandle)).previousNewSelections.isEmpty)
            .isTrue()
    }

    @Test
    fun `serialized saved state restores selection history`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

        val restoredHolder = DefaultEmbeddedSelectionHolder(restoreSavedState(savedStateHandle))

        assertThat(restoredHolder.getPreviousNewSelection("card"))
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(restoredHolder.getPreviousNewSelection("cashapp")?.paymentMethodCreateParams?.toParamMap())
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION.paymentMethodCreateParams.toParamMap())
        val restoredSelection = restoredHolder.selection.value as PaymentSelection.New
        assertThat(restoredSelection.paymentMethodType).isEqualTo("cashapp")
        assertThat(restoredSelection.paymentMethodCreateParams.toParamMap())
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION.paymentMethodCreateParams.toParamMap())
    }

    private fun restoreSavedState(handle: SavedStateHandle): SavedStateHandle {
        val parcel = Parcel.obtain()
        try {
            handle.savedStateProvider().saveState().writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val restored = Bundle.CREATOR.createFromParcel(parcel).apply {
                classLoader = PreviousNewSelections::class.java.classLoader
            }
            return SavedStateHandle.createHandle(restored, null)
        } finally {
            parcel.recycle()
        }
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
