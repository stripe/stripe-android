package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.R
import org.junit.Test

class PaymentMethodsUiExtensionTest {

    @Test
    fun `getSublabel returns card art program name`() {
        val paymentMethod = createCardPaymentMethod(programName = "Test Program")

        assertThat(paymentMethod.getSublabel()).isEqualTo("Test Program".resolvableString)
    }

    @Test
    fun `getSublabel returns null for blank card art program name`() {
        val paymentMethod = createCardPaymentMethod(programName = "   ")

        assertThat(paymentMethod.getSublabel()).isNull()
    }

    @Test
    fun `getSublabel returns null for null card art program name`() {
        val paymentMethod = createCardPaymentMethod(programName = null)

        assertThat(paymentMethod.getSublabel()).isNull()
    }

    @Test
    fun `getSublabel prioritizes Link payment details over card art program name`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = createCard(programName = "Test Program"),
            linkPaymentDetails = PaymentMethodFixtures.LINK_PAYMENT_METHOD.linkPaymentDetails,
        )

        assertThat(paymentMethod.getSublabel()).isEqualTo(
            resolvableString(R.string.stripe_link_payment_method_last4, "4242")
        )
    }

    private fun createCardPaymentMethod(programName: String?): PaymentMethod {
        return PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = createCard(programName = programName),
        )
    }

    private fun createCard(programName: String?): PaymentMethod.Card {
        return PaymentMethodFixtures.CARD.copy(
            cardArt = PaymentMethod.Card.CardArt(
                artImage = null,
                programName = programName,
            )
        )
    }
}
