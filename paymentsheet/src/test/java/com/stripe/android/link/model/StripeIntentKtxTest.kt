package com.stripe.android.link.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripeIntentKtxTest {

    @Test
    fun `When consumer types is empty then returns all funding sources`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("card", "bank_account"))
            .supportedPaymentMethodTypes(consumerSupportedPaymentDetailsTypes = emptyList())
        assertThat(supportedTypes).containsExactly("card", "bank_account")
    }

    @Test
    fun `When funding sources contains card then returns card`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("card"))
            .supportedPaymentMethodTypes(consumerSupportedPaymentDetailsTypes = listOf("CARD"))
        assertThat(supportedTypes).containsExactly("card")
    }

    @Test
    fun `When funding sources contains card and bank_account then returns both`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("card", "bank_account"))
            .supportedPaymentMethodTypes(consumerSupportedPaymentDetailsTypes = listOf("CARD", "BANK_ACCOUNT"))
        assertThat(supportedTypes).containsExactly("card", "bank_account")
    }

    @Test
    fun `When funding sources contains generic types then returns them`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("card", "crypto", "pix"))
            .supportedPaymentMethodTypes(consumerSupportedPaymentDetailsTypes = listOf("CARD", "CRYPTO", "PIX"))
        assertThat(supportedTypes).containsExactly("card", "crypto", "pix")
    }

    @Test
    fun `When funding sources contains only generic types then returns them`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("crypto", "pix"))
            .supportedPaymentMethodTypes(consumerSupportedPaymentDetailsTypes = listOf("CRYPTO", "PIX"))
        assertThat(supportedTypes).containsExactly("crypto", "pix")
    }

    @Test
    fun `When funding sources is empty then returns empty set`() {
        val supportedTypes = stripeIntent(fundingSources = emptyList())
            .supportedPaymentMethodTypes(consumerSupportedPaymentDetailsTypes = listOf("CARD"))
        assertThat(supportedTypes).isEmpty()
    }

    @Test
    fun `Intersection is case-insensitive`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("card", "bank_account"))
            .supportedPaymentMethodTypes(consumerSupportedPaymentDetailsTypes = listOf("CARD"))
        assertThat(supportedTypes).containsExactly("card")
    }

    @Test
    fun `Returns empty when intersection is empty`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("bank_account"))
            .supportedPaymentMethodTypes(consumerSupportedPaymentDetailsTypes = listOf("CARD"))
        assertThat(supportedTypes).isEmpty()
    }

    @Test
    fun `Intersection excludes consumer types not in funding sources`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("card"))
            .supportedPaymentMethodTypes(
                consumerSupportedPaymentDetailsTypes = listOf("CARD", "BANK_ACCOUNT", "KLARNA")
            )
        assertThat(supportedTypes).containsExactly("card")
    }

    private fun stripeIntent(
        fundingSources: List<String> = emptyList(),
    ) = PaymentIntentFixtures.PI_SUCCEEDED.copy(
        linkFundingSources = fundingSources,
    )
}
