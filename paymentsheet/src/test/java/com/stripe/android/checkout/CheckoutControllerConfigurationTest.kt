package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutControllerConfigurationTest {
    @Test
    fun `api configuration is null by default`() {
        val state = CheckoutController.Configuration().build()

        assertThat(state.apiConfiguration).isNull()
    }

    @Test
    fun `api configuration builds requested credentials`() {
        val state = CheckoutController.Configuration()
            .apiConfiguration(
                ApiConfiguration("pk_test_123")
                    .stripeAccountId("acct_123")
            )
            .build()

        assertThat(state.apiConfiguration?.publishableKey).isEqualTo("pk_test_123")
        assertThat(state.apiConfiguration?.stripeAccountId).isEqualTo("acct_123")
    }
}
