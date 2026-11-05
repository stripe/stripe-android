package com.stripe.android.crypto.onramp.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

class OnrampConfigurationTest {
    @Test
    fun `Link card brand acceptance defaults to all`() {
        assertThat(configuration().build().linkCardBrandAcceptance)
            .isEqualTo(PaymentSheet.CardBrandAcceptance.all())
    }

    @Test
    fun `Link card brand acceptance preserves allowed brands`() {
        val acceptance = PaymentSheet.CardBrandAcceptance.allowed(
            listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa)
        )
        assertThat(configuration().linkCardBrandAcceptance(acceptance).build().linkCardBrandAcceptance)
            .isEqualTo(acceptance)
    }

    @Test
    fun `Google Pay card brand acceptance defaults to all`() {
        assertThat(configuration().build().googlePayCardBrandAcceptance)
            .isEqualTo(PaymentSheet.CardBrandAcceptance.all())
    }

    @Test
    fun `Google Pay and Link card brand acceptance are independent`() {
        val googlePayAcceptance = PaymentSheet.CardBrandAcceptance.allowed(
            listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa)
        )
        val linkAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
            listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Amex)
        )
        val state = configuration()
            .googlePayCardBrandAcceptance(googlePayAcceptance)
            .linkCardBrandAcceptance(linkAcceptance)
            .build()

        assertThat(state.googlePayCardBrandAcceptance).isEqualTo(googlePayAcceptance)
        assertThat(state.linkCardBrandAcceptance).isEqualTo(linkAcceptance)
    }

    private fun configuration() = OnrampConfiguration()
        .merchantDisplayName("Merchant")
        .publishableKey("pk_test_123")
}
