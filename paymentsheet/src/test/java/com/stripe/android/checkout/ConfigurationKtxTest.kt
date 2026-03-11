package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import org.junit.Test

class ConfigurationKtxTest {

    @Test
    fun `sets email from checkout session when defaultBillingDetails is null`() {
        val config = configuration()
        val response = checkoutSessionResponse(customerEmail = "test@example.com")

        val result = config.forCheckoutSession(response)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("test@example.com")
    }

    @Test
    fun `preserves merchant email when already set`() {
        val config = configuration(
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "merchant@example.com"),
        )
        val response = checkoutSessionResponse(customerEmail = "checkout@example.com")

        val result = config.forCheckoutSession(response)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("merchant@example.com")
    }

    @Test
    fun `returns unchanged config when both emails are null`() {
        val config = configuration()
        val response = checkoutSessionResponse(customerEmail = null)

        val result = config.forCheckoutSession(response)

        assertThat(result.defaultBillingDetails?.email).isNull()
    }

    @Test
    fun `preserves existing billing details fields when setting email`() {
        val address = PaymentSheet.Address(
            city = "San Francisco",
            country = "US",
        )
        val config = configuration(
            defaultBillingDetails = PaymentSheet.BillingDetails(
                address = address,
                name = "Jane Doe",
                phone = "5551234567",
            ),
        )
        val response = checkoutSessionResponse(customerEmail = "test@example.com")

        val result = config.forCheckoutSession(response)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("test@example.com")
        assertThat(result.defaultBillingDetails?.name).isEqualTo("Jane Doe")
        assertThat(result.defaultBillingDetails?.phone).isEqualTo("5551234567")
        assertThat(result.defaultBillingDetails?.address).isEqualTo(address)
    }

    private fun configuration(
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration.Builder("Test Merchant")
            .defaultBillingDetails(defaultBillingDetails)
            .build()
    }

    private fun checkoutSessionResponse(
        customerEmail: String? = null,
    ): CheckoutSessionResponse {
        return CheckoutSessionResponse(
            id = "cs_test_abc123",
            amount = 1000L,
            currency = "usd",
            customerEmail = customerEmail,
        )
    }
}
