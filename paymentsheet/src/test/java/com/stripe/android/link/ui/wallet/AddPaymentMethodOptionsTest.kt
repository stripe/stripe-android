package com.stripe.android.link.ui.wallet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentMethodFilter
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import org.junit.Test

class AddPaymentMethodOptionsTest {

    private fun createOptions(
        linkAccount: LinkAccount = TestFactory.LINK_ACCOUNT_WITH_PK,
        fundingSources: List<String> = listOf(
            ConsumerPaymentDetails.Card.TYPE,
            ConsumerPaymentDetails.BankAccount.TYPE
        ),
        financialConnectionsAvailability: FinancialConnectionsAvailability? =
            FinancialConnectionsAvailability.Full,
        paymentMethodFilter: LinkPaymentMethodFilter? = null,
        linkLaunchMode: LinkLaunchMode =
            LinkLaunchMode.PaymentMethodSelection(null, paymentMethodFilter)
    ): AddPaymentMethodOptions {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                linkFundingSources = fundingSources
            ),
            financialConnectionsAvailability = financialConnectionsAvailability
        )

        return AddPaymentMethodOptions(
            linkAccount = linkAccount,
            configuration = configuration,
            linkLaunchMode = linkLaunchMode
        )
    }

    @Test
    fun `values includes both card and bank when all conditions are met`() {
        val options = createOptions()

        assertThat(options.values).hasSize(2)
        assertThat(options.values).containsExactly(
            AddPaymentMethodOption.Bank(FinancialConnectionsAvailability.Full),
            AddPaymentMethodOption.Card
        )
    }

    @Test
    fun `values includes only card when bank conditions are not met - no publishable key`() {
        val options = createOptions(linkAccount = TestFactory.LINK_ACCOUNT)

        assertThat(options.values).hasSize(1)
        assertThat(options.values).containsExactly(AddPaymentMethodOption.Card)
    }

    @Test
    fun `values includes only card when bank conditions are not met - no financial connections availability`() {
        val options = createOptions(financialConnectionsAvailability = null)

        assertThat(options.values).hasSize(1)
        assertThat(options.values).containsExactly(AddPaymentMethodOption.Card)
    }

    @Test
    fun `values includes only card when bank account type not supported`() {
        val options = createOptions(fundingSources = listOf(ConsumerPaymentDetails.Card.TYPE))

        assertThat(options.values).hasSize(1)
        assertThat(options.values).containsExactly(AddPaymentMethodOption.Card)
    }

    @Test
    fun `values respects payment method filter - card only`() {
        val options = createOptions(paymentMethodFilter = LinkPaymentMethodFilter.Card)

        assertThat(options.values).hasSize(1)
        assertThat(options.values).containsExactly(AddPaymentMethodOption.Card)
    }

    @Test
    fun `values respects payment method filter - bank account only`() {
        val options = createOptions(paymentMethodFilter = LinkPaymentMethodFilter.BankAccount)

        assertThat(options.values).hasSize(1)
        assertThat(options.values).containsExactly(
            AddPaymentMethodOption.Bank(FinancialConnectionsAvailability.Full)
        )
    }

    @Test
    fun `values handles empty funding sources with card fallback`() {
        val options = createOptions(fundingSources = emptyList())

        assertThat(options.values).hasSize(1)
        assertThat(options.values).containsExactly(AddPaymentMethodOption.Card)
    }

    @Test
    fun `values handles non-PaymentMethodSelection launch mode`() {
        val options = createOptions(linkLaunchMode = LinkLaunchMode.Full)

        assertThat(options.values).hasSize(2)
        assertThat(options.values).containsExactly(
            AddPaymentMethodOption.Bank(FinancialConnectionsAvailability.Full),
            AddPaymentMethodOption.Card
        )
    }

    @Test
    fun `values works with FinancialConnectionsAvailability Lite`() {
        val options = createOptions(
            financialConnectionsAvailability = FinancialConnectionsAvailability.Lite
        )

        assertThat(options.values).hasSize(2)
        assertThat(options.values).containsExactly(
            AddPaymentMethodOption.Bank(FinancialConnectionsAvailability.Lite),
            AddPaymentMethodOption.Card
        )
    }

    @Test
    fun `default returns Card when multiple options available`() {
        val options = createOptions()

        assertThat(options.default).isEqualTo(AddPaymentMethodOption.Card)
    }

    @Test
    fun `default returns first option when only one available`() {
        val options = createOptions(
            fundingSources = listOf(ConsumerPaymentDetails.BankAccount.TYPE),
            paymentMethodFilter = LinkPaymentMethodFilter.BankAccount
        )

        assertThat(options.default).isEqualTo(
            AddPaymentMethodOption.Bank(FinancialConnectionsAvailability.Full)
        )
    }

    @Test
    fun `default returns card when no funding sources supported`() {
        val options = createOptions(fundingSources = emptyList())

        assertThat(options.default).isEqualTo(AddPaymentMethodOption.Card)
    }

    @Test
    fun `default returns null when no options available`() {
        val options = createOptions(
            linkAccount = TestFactory.LINK_ACCOUNT,
            fundingSources = listOf(ConsumerPaymentDetails.BankAccount.TYPE),
            financialConnectionsAvailability = null
        )

        assertThat(options.default).isNull()
    }
}
