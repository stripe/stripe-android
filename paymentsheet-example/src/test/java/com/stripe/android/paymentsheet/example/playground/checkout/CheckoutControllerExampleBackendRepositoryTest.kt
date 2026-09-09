package com.stripe.android.paymentsheet.example.playground.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions.session
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import org.junit.Test

class CheckoutControllerExampleBackendRepositoryTest {
    @Test
    fun `selected merchant is used`() {
        val settings = CheckoutPlaygroundSettings.createInMemory().apply {
            update(session.merchant, Merchant.JP)
        }

        assertThat(settings.snapshot().backendMerchant()).isEqualTo(Merchant.JP)
    }

    @Test
    fun `automatic tax uses tax merchant`() {
        val settings = CheckoutPlaygroundSettings.createInMemory().apply {
            update(session.merchant, Merchant.JP)
            update(session.automaticTax, true)
        }

        assertThat(settings.snapshot().backendMerchant()).isEqualTo(Merchant.US_TAX)
    }
}
