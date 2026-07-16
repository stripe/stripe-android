package com.stripe.android.paymentelement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import kotlin.test.Test

internal class MutableApiRequestOptionsProviderTest {

    private val fallbackPublishableKey = "pk_test_fallback"
    private val fallbackStripeAccountId = "acct_fallback"

    private val provider = MutableApiRequestOptionsProvider(
        paymentConfig = {
            PaymentConfiguration(
                publishableKey = fallbackPublishableKey,
                stripeAccountId = fallbackStripeAccountId,
            )
        }
    )

    @Test
    fun `get returns options from PaymentConfiguration when apiConfiguration is null`() {
        val options = provider.get()

        assertThat(options.apiKey).isEqualTo(fallbackPublishableKey)
        assertThat(options.stripeAccount).isEqualTo(fallbackStripeAccountId)
    }

    @Test
    fun `get returns options from ApiConfiguration when it is set`() {
        val apiConfiguration = ApiConfiguration(
            publishableKey = "pk_test_custom",
            stripeAccountId = "acct_custom",
        )
        provider.update(apiConfiguration)

        val options = provider.get()

        assertThat(options.apiKey).isEqualTo("pk_test_custom")
        assertThat(options.stripeAccount).isEqualTo("acct_custom")
    }

    @Test
    fun `get returns options from ApiConfiguration with null stripeAccountId`() {
        val apiConfiguration = ApiConfiguration(publishableKey = "pk_test_custom")
        provider.update(apiConfiguration)

        val options = provider.get()

        assertThat(options.apiKey).isEqualTo("pk_test_custom")
        assertThat(options.stripeAccount).isNull()
    }

    @Test
    fun `get falls back to PaymentConfiguration after update with null`() {
        provider.update(ApiConfiguration(publishableKey = "pk_test_custom"))
        provider.update(null)

        val options = provider.get()

        assertThat(options.apiKey).isEqualTo(fallbackPublishableKey)
        assertThat(options.stripeAccount).isEqualTo(fallbackStripeAccountId)
    }
}
