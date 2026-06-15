package com.stripe.android.link.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripeIntentKtxTest {

    @Test
    fun `When funding sources contains card then returns card`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("card"))
            .supportedPaymentMethodTypes()
        assertThat(supportedTypes).containsExactly("card")
    }

    @Test
    fun `When funding sources contains card and bank_account then returns both`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("card", "bank_account"))
            .supportedPaymentMethodTypes()
        assertThat(supportedTypes).containsExactly("card", "bank_account")
    }

    @Test
    fun `When funding sources contains generic types then returns them`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("card", "crypto", "pix"))
            .supportedPaymentMethodTypes()
        assertThat(supportedTypes).containsExactly("card", "crypto", "pix")
    }

    @Test
    fun `When funding sources contains only generic types then returns them`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("crypto", "pix"))
            .supportedPaymentMethodTypes()
        assertThat(supportedTypes).containsExactly("crypto", "pix")
    }

    @Test
    fun `When funding sources is empty then defaults to card`() {
        val supportedTypes = stripeIntent(fundingSources = emptyList())
            .supportedPaymentMethodTypes()
        assertThat(supportedTypes).containsExactly("card")
    }

    private fun stripeIntent(
        fundingSources: List<String> = emptyList(),
    ) = PaymentIntentFixtures.PI_SUCCEEDED.copy(
        linkFundingSources = fundingSources,
    )
}
