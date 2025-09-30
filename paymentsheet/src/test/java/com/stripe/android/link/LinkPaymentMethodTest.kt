package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
import com.stripe.android.link.TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
import com.stripe.android.model.CvcCheck
import org.junit.Test

class LinkPaymentMethodTest {

    @Test
    fun `readyForConfirmation returns true for BankAccount`() {
        val bankAccount = CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT
        val paymentMethod = LinkPaymentMethod.ConsumerPaymentDetails(
            bankAccount,
            collectedCvc = null,
            billingPhone = null
        )
        assertThat(paymentMethod.readyForConfirmation()).isTrue()
    }

    @Test
    fun `readyForConfirmation returns false for expired card`() {
        val card = CONSUMER_PAYMENT_DETAILS_CARD.copy(
            expiryYear = 1990,
            expiryMonth = 12,
            cvcCheck = CvcCheck.Pass
        )
        val paymentMethod = LinkPaymentMethod.ConsumerPaymentDetails(
            details = card,
            collectedCvc = "123",
            billingPhone = null
        )
        assertThat(paymentMethod.readyForConfirmation()).isFalse()
    }

    @Test
    fun `readyForConfirmation returns false when cvc recollection required and cvc is empty`() {
        val card = CONSUMER_PAYMENT_DETAILS_CARD.copy(
            expiryYear = 2100,
            expiryMonth = 12,
            cvcCheck = CvcCheck.Fail
        )
        val paymentMethod = LinkPaymentMethod.ConsumerPaymentDetails(
            card,
            collectedCvc = "",
            billingPhone = null
        )
        assertThat(paymentMethod.readyForConfirmation()).isFalse()
    }

    @Test
    fun `readyForConfirmation returns true when cvc recollection required and cvc is provided`() {
        val card = CONSUMER_PAYMENT_DETAILS_CARD.copy(
            expiryYear = 2100,
            expiryMonth = 12,
            cvcCheck = CvcCheck.Fail
        )
        val paymentMethod = LinkPaymentMethod.ConsumerPaymentDetails(
            details = card,
            collectedCvc = "123",
            billingPhone = null
        )
        assertThat(paymentMethod.readyForConfirmation()).isTrue()
    }
}
