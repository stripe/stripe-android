package com.stripe.android.link.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.TestFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripeIntentKtxTest {

    @Test
    fun `When test mode and test account then all funding sources are enabled`() {
        val supportedTypes = stripeIntent(liveMode = false)
            .supportedPaymentMethodTypes(linkAccount("test+multiple_funding_sources@test.abc"))
        assertThat(supportedTypes).containsExactly("card", "bank_account")
    }

    @Test
    fun `When live mode and test account then uses intent funding sources`() {
        val supportedTypes = stripeIntent(fundingSources = listOf("card"))
            .supportedPaymentMethodTypes(linkAccount("test+multiple_funding_sources@test.abc"))
        assertThat(supportedTypes).containsExactly("card")
    }

    @Test
    fun `When test mode and not test account then uses intent funding sources`() {
        val supportedTypes = stripeIntent(listOf("card"))
            .supportedPaymentMethodTypes(linkAccount("test@test.abc"))
        assertThat(supportedTypes).containsExactly("card")
    }

    @Test
    fun `When funding sources is empty then default to card`() {
        val supportedTypes = stripeIntent()
            .supportedPaymentMethodTypes(linkAccount("test+multiple_funding_sources@test.abc"))
        assertThat(supportedTypes).containsExactly("card")
    }

    @Test
    fun `When funding sources contains invalid items then invalid items are ignored`() {
        val supportedTypes = stripeIntent(listOf("invalid", "invalid2", "bank_account"))
            .supportedPaymentMethodTypes(linkAccount("test+multiple_funding_sources@test.abc"))
        assertThat(supportedTypes).containsExactly("bank_account")
    }

    @Test
    fun `When funding sources contains only invalid items then default to card`() {
        val supportedTypes = stripeIntent(listOf("invalid", "invalid2", "bank_acc0unt"))
            .supportedPaymentMethodTypes(linkAccount("test+multiple_funding_sources@test.abc"))
        assertThat(supportedTypes).containsExactly("card")
    }

    private fun stripeIntent(
        fundingSources: List<String> = emptyList(),
        liveMode: Boolean = true
    ) = StripeIntentFixtures.PI_SUCCEEDED.copy(
        linkFundingSources = fundingSources,
        isLiveMode = liveMode
    )

    private fun linkAccount(accountEmail: String) = LinkAccount(
        consumerSession = TestFactory.CONSUMER_SESSION.copy(
            emailAddress = accountEmail
        )
    )
}
