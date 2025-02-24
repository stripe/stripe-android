package com.stripe.android.paymentsheet.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import org.junit.Test

class PaymentMethodIncentiveTest {

    @Test
    fun `Instant Debits incentive matches Link payment method`() {
        val paymentMethodIncentive = PaymentMethodIncentive(
            identifier = "link_instant_debits",
            displayText = "$5"
        )

        val applicableIncentive = paymentMethodIncentive.takeIfMatches(PaymentMethod.Type.Link.code)
        assertThat(applicableIncentive).isNotNull()
    }

    @Test
    fun `Instant Debits incentive does not match other payment methods`() {
        val paymentMethodIncentive = PaymentMethodIncentive(
            identifier = "link_instant_debits",
            displayText = "$5"
        )

        val applicableIncentive = paymentMethodIncentive.takeIfMatches(PaymentMethod.Type.USBankAccount.code)
        assertThat(applicableIncentive).isNull()
    }

    @Test
    fun `Unknown incentive does not match any payment method`() {
        val paymentMethodIncentive = PaymentMethodIncentive(
            identifier = "a_weird_payment_method",
            displayText = "$5"
        )

        val matches = PaymentMethod.Type.entries.mapNotNull {
            paymentMethodIncentive.takeIfMatches(it.code)
        }

        assertThat(matches).isEmpty()
    }
}
