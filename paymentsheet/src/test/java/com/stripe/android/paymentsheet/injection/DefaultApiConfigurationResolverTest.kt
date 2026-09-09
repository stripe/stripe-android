package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultApiConfigurationResolverTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        PaymentConfiguration.clearInstance()
    }

    @Test
    fun `resolve uses provided API configuration before PaymentConfiguration`() {
        PaymentConfiguration.init(
            context = context,
            publishableKey = "pk_test_from_payment_configuration",
            stripeAccountId = "acct_from_payment_configuration",
        )
        val apiConfiguration = ApiConfiguration.State(
            publishableKey = "pk_test_from_api_configuration",
            stripeAccountId = "acct_from_api_configuration",
        )

        val resolvedApiConfiguration = DefaultApiConfigurationResolver(context).resolve(apiConfiguration)

        assertThat(resolvedApiConfiguration).isEqualTo(apiConfiguration)
    }

    @Test
    fun `resolve falls back to PaymentConfiguration if apiConfiguration is null`() {
        PaymentConfiguration.init(
            context = context,
            publishableKey = "pk_test_from_payment_configuration",
            stripeAccountId = "acct_from_payment_configuration",
        )

        val resolvedApiConfiguration = DefaultApiConfigurationResolver(context).resolve(apiConfiguration = null)

        assertThat(resolvedApiConfiguration).isEqualTo(
            ApiConfiguration.State(
                publishableKey = "pk_test_from_payment_configuration",
                stripeAccountId = "acct_from_payment_configuration",
            )
        )
    }
}
