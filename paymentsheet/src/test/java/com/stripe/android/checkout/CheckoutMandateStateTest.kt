package com.stripe.android.checkout

import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutMandateStateTest {
    @Test
    fun `content access is restored with the controller state after process death`() = runScenario {
        mandateState.recordContentAccess()

        val restoredState = restoreMandateState()

        assertThat(restoredState).isNotSameInstanceAs(mandateState)
        assertThat(restoredState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
    }

    @Test
    fun `mandate access is restored with the controller state for the same payment method`() = runScenario {
        mandateState.recordMandateTextAccess(SAVED_SEPA.paymentMethod)

        val restoredState = restoreMandateState()

        assertThat(restoredState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
        assertThat(
            restoredState.hasAcknowledgedSepaMandate(
                PaymentSelection.Saved(SAVED_SEPA.paymentMethod.copy(id = "pm_another_sepa"))
            )
        ).isFalse()
    }

    @Test
    fun `sheet acknowledgement is restored for the accepted payment method after accessing another mandate`() =
        runScenario {
            mandateState.acknowledgeSepaMandate(SAVED_SEPA.paymentMethod)
            mandateState.recordMandateTextAccess(SAVED_SEPA.paymentMethod.copy(id = "pm_another_sepa"))

            val restoredState = restoreMandateState()

            assertThat(restoredState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
            assertThat(
                restoredState.hasAcknowledgedSepaMandate(
                    PaymentSelection.Saved(SAVED_SEPA.paymentMethod.copy(id = "pm_unacknowledged_sepa"))
                )
            ).isFalse()
        }

    @Test
    fun `accessing a non-SEPA mandate does not acknowledge saved SEPA with the same ID`() = runScenario {
        mandateState.recordMandateTextAccess(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = SAVED_SEPA.paymentMethod.id)
        )

        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle)
        val state = CheckoutControllerStateFactory.create()
        stateHolder.state = state
        Scenario(savedStateHandle, state.mandateState).block()
    }

    private class Scenario(
        val savedStateHandle: SavedStateHandle,
        val mandateState: CheckoutMandateState,
    )

    private fun Scenario.restoreMandateState(): CheckoutMandateState {
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle.restoreFromParcel())
        return requireNotNull(stateHolder.state).mandateState
    }

    @Suppress("RestrictedApi")
    private fun SavedStateHandle.restoreFromParcel(): SavedStateHandle {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(savedStateProvider().saveState())
            parcel.setDataPosition(0)
            SavedStateHandle.createHandle(parcel.readBundle(CheckoutMandateState::class.java.classLoader), null)
        } finally {
            parcel.recycle()
        }
    }

    private companion object {
        val SAVED_SEPA = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
    }
}
