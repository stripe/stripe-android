package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class CheckoutMandateStateTest {
    @Test
    fun `content access acknowledges subsequently selected saved SEPA methods`() {
        val mandateState = CheckoutMandateState.initial()
        mandateState.recordContentAccess()

        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
        assertThat(
            mandateState.hasAcknowledgedSepaMandate(
                PaymentSelection.Saved(SAVED_SEPA.paymentMethod.copy(id = "pm_another_sepa"))
            )
        ).isTrue()
    }

    @Test
    fun `mandate access acknowledges only the same payment method`() {
        val mandateState = CheckoutMandateState.initial()
        mandateState.recordMandateTextAccess(SAVED_SEPA.paymentMethod)

        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
        assertThat(
            mandateState.hasAcknowledgedSepaMandate(
                PaymentSelection.Saved(SAVED_SEPA.paymentMethod.copy(id = "pm_another_sepa"))
            )
        ).isFalse()
    }

    @Test
    fun `sheet acknowledgement is preserved after accessing another mandate`() {
        val mandateState = CheckoutMandateState.initial()
        mandateState.acknowledgeSepaMandate(SAVED_SEPA.paymentMethod)
        mandateState.recordMandateTextAccess(SAVED_SEPA.paymentMethod.copy(id = "pm_another_sepa"))

        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isTrue()
        assertThat(
            mandateState.hasAcknowledgedSepaMandate(
                PaymentSelection.Saved(SAVED_SEPA.paymentMethod.copy(id = "pm_another_sepa"))
            )
        ).isTrue()
        assertThat(
            mandateState.hasAcknowledgedSepaMandate(
                PaymentSelection.Saved(SAVED_SEPA.paymentMethod.copy(id = "pm_unacknowledged_sepa"))
            )
        ).isFalse()
    }

    @Test
    fun `accessing a non-SEPA mandate does not acknowledge saved SEPA with the same ID`() {
        val mandateState = CheckoutMandateState.initial()
        mandateState.recordMandateTextAccess(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = SAVED_SEPA.paymentMethod.id)
        )

        assertThat(mandateState.hasAcknowledgedSepaMandate(SAVED_SEPA)).isFalse()
    }

    private companion object {
        val SAVED_SEPA = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
    }
}
