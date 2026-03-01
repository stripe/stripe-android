package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
class CheckoutSessionKtTest {

    @Test
    fun `asCheckoutSession maps id correctly`() {
        val response = CheckoutSessionResponse(
            id = "cs_test_123",
            amount = 5000L,
            currency = "usd",
        )

        val checkoutSession = response.asCheckoutSession()

        assertThat(checkoutSession.id).isEqualTo("cs_test_123")
    }
}
