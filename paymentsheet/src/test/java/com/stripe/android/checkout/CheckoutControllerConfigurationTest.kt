package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
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

    @Test
    fun `payment element Google Pay configuration is null when payment element is not configured`() {
        val configuration = CheckoutController.Configuration().build()
        val response = CheckoutSessionResponseFactory.create(merchantCountry = "US")

        assertThat(configuration.toPaymentElementGooglePayConfiguration(response)).isNull()
    }
}
