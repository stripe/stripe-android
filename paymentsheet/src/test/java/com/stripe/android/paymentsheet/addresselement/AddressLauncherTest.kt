package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import org.junit.Test

class AddressLauncherTest {

    @Test
    fun `resolveStripeAccountIdForLaunch returns stripe account for matching publishable key`() {
        val stripeAccountId = resolveStripeAccountIdForLaunch(
            publishableKey = "pk_test_123",
            paymentConfiguration = PaymentConfiguration(
                publishableKey = "pk_test_123",
                stripeAccountId = "acct_123",
            ),
        )

        assertThat(stripeAccountId).isEqualTo("acct_123")
    }

    @Test
    fun `resolveStripeAccountIdForLaunch ignores stripe account for different publishable key`() {
        val stripeAccountId = resolveStripeAccountIdForLaunch(
            publishableKey = "pk_test_123",
            paymentConfiguration = PaymentConfiguration(
                publishableKey = "pk_test_456",
                stripeAccountId = "acct_123",
            ),
        )

        assertThat(stripeAccountId).isNull()
    }
}
