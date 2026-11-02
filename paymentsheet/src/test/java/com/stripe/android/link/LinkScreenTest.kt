package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.PaymentIntentFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkScreenTest {

    @Test
    fun `walletScreen returns Wallet when consumer types overlap with funding sources`() {
        val screen = configuration(fundingSources = listOf("card", "bank_account"))
            .walletScreen(linkAccount(consumerTypes = listOf("CARD")), FULL)

        assertThat(screen).isEqualTo(LinkScreen.Wallet)
    }

    @Test
    fun `walletScreen returns NoFundingSources when consumer types don't overlap with funding sources`() {
        val screen = configuration(fundingSources = listOf("bank_account"))
            .walletScreen(linkAccount(consumerTypes = listOf("CARD")), FULL)

        assertThat(screen).isEqualTo(LinkScreen.NoFundingSources)
    }

    @Test
    fun `walletScreen returns Wallet when consumer types aren't known`() {
        val screen = configuration(fundingSources = emptyList())
            .walletScreen(linkAccount(consumerTypes = emptyList()), FULL)

        assertThat(screen).isEqualTo(LinkScreen.Wallet)
    }

    @Test
    fun `walletScreen returns NoFundingSources when payment method filters exclude all supported types`() {
        val screen = configuration(fundingSources = listOf("card", "bank_account"))
            .walletScreen(
                linkAccount = linkAccount(consumerTypes = listOf("CARD")),
                linkLaunchMode = paymentMethodSelection(filters = listOf(LinkPaymentMethodFilter.BankAccount)),
            )

        assertThat(screen).isEqualTo(LinkScreen.NoFundingSources)
    }

    @Test
    fun `walletScreen returns Wallet when payment method filters allow a supported type`() {
        val screen = configuration(fundingSources = listOf("card", "bank_account"))
            .walletScreen(
                linkAccount = linkAccount(consumerTypes = listOf("CARD", "BANK_ACCOUNT")),
                linkLaunchMode = paymentMethodSelection(filters = listOf(LinkPaymentMethodFilter.BankAccount)),
            )

        assertThat(screen).isEqualTo(LinkScreen.Wallet)
    }

    @Test
    fun `walletScreen returns Wallet when payment method filters are null`() {
        val screen = configuration(fundingSources = listOf("card"))
            .walletScreen(
                linkAccount = linkAccount(consumerTypes = listOf("CARD")),
                linkLaunchMode = paymentMethodSelection(filters = null),
            )

        assertThat(screen).isEqualTo(LinkScreen.Wallet)
    }

    @Test
    fun `walletScreen applies Generic filter to non-card and non-bank types`() {
        val screen = configuration(fundingSources = listOf("card", "crypto"))
            .walletScreen(
                linkAccount = linkAccount(consumerTypes = listOf("CARD", "CRYPTO")),
                linkLaunchMode = paymentMethodSelection(filters = listOf(LinkPaymentMethodFilter.Generic)),
            )

        assertThat(screen).isEqualTo(LinkScreen.Wallet)
    }

    private fun paymentMethodSelection(filters: List<LinkPaymentMethodFilter>?): LinkLaunchMode {
        return LinkLaunchMode.PaymentMethodSelection(selectedPayment = null, paymentMethodFilters = filters)
    }

    private fun configuration(fundingSources: List<String>): LinkConfiguration {
        return TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(linkFundingSources = fundingSources)
        )
    }

    private fun linkAccount(consumerTypes: List<String>): LinkAccount {
        return LinkAccount(TestFactory.CONSUMER_SESSION.copy(supportedPaymentDetailsTypes = consumerTypes))
    }

    private companion object {
        val FULL = LinkLaunchMode.Full(showSecondaryButton = true)
    }
}
