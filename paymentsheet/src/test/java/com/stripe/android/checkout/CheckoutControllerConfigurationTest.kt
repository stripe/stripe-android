package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutControllerConfigurationTest {
    @Test
    fun `payment element is not configured by default`() {
        val configuration = CheckoutController.Configuration().build()

        assertThat(configuration.paymentElementConfiguration).isNull()
    }

    @Test
    fun `payment element configuration is included when explicitly configured`() {
        val paymentElement = PaymentElement.Configuration().paymentMethodOrder(listOf("klarna", "card"))
        val configuration = CheckoutController.Configuration()
            .paymentElement(paymentElement)
            .build()

        assertThat(configuration.paymentElementConfiguration).isEqualTo(paymentElement.build())
    }
}
