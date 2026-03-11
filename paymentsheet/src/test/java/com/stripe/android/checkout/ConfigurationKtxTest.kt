package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
class ConfigurationKtxTest {

    @Test
    fun `sets email from checkout session when defaultBillingDetails is null`() {
        val config = configuration()
        val state = state(customerEmail = "test@example.com")

        val result = config.forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("test@example.com")
    }

    @Test
    fun `preserves merchant email when already set`() {
        val config = configuration(
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "merchant@example.com"),
        )
        val state = state(customerEmail = "checkout@example.com")

        val result = config.forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("merchant@example.com")
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
        val state = state(customerEmail = "test@example.com")

        val result = config.forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("test@example.com")
        assertThat(result.defaultBillingDetails?.name).isEqualTo("Jane Doe")
        assertThat(result.defaultBillingDetails?.phone).isEqualTo("5551234567")
        assertThat(result.defaultBillingDetails?.address).isEqualTo(address)
    }

    @Test
    fun `sets shipping name from state when shippingDetails is null`() {
        val config = configuration()
        val state = state(shippingName = "John Doe")

        val result = config.forCheckoutSession(state)

        assertThat(result.shippingDetails?.name).isEqualTo("John Doe")
    }

    @Test
    fun `preserves merchant shipping name when already set`() {
        val config = configuration(
            shippingDetails = AddressDetails(name = "Merchant Name"),
        )
        val state = state(shippingName = "State Name")

        val result = config.forCheckoutSession(state)

        assertThat(result.shippingDetails?.name).isEqualTo("Merchant Name")
    }

    @Test
    fun `preserves existing shipping details fields when setting name`() {
        val address = PaymentSheet.Address(
            city = "San Francisco",
            country = "US",
        )
        val config = configuration(
            shippingDetails = AddressDetails(
                address = address,
                phoneNumber = "5551234567",
                isCheckboxSelected = true,
            ),
        )
        val state = state(shippingName = "John Doe")

        val result = config.forCheckoutSession(state)

        assertThat(result.shippingDetails?.name).isEqualTo("John Doe")
        assertThat(result.shippingDetails?.address).isEqualTo(address)
        assertThat(result.shippingDetails?.phoneNumber).isEqualTo("5551234567")
        assertThat(result.shippingDetails?.isCheckboxSelected).isTrue()
    }

    @Test
    fun `sets both email and shipping name simultaneously`() {
        val config = configuration()
        val state = state(customerEmail = "test@example.com", shippingName = "John Doe")

        val result = config.forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("test@example.com")
        assertThat(result.shippingDetails?.name).isEqualTo("John Doe")
    }

    private fun configuration(
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
        shippingDetails: AddressDetails? = null,
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration.Builder("Test Merchant")
            .defaultBillingDetails(defaultBillingDetails)
            .shippingDetails(shippingDetails)
            .build()
    }

    private fun state(
        customerEmail: String? = null,
        shippingName: String? = null,
    ): Checkout.State {
        return Checkout.State(
            checkoutSessionResponse = CheckoutSessionResponse(
                id = "cs_test_abc123",
                amount = 1000L,
                currency = "usd",
                customerEmail = customerEmail,
            elementsSession = null,
            paymentIntent = null,
            customer = null,
            savedPaymentMethodsOfferSave = null,
            totalSummary = null,
            lineItems = emptyList(),
            shippingOptions = emptyList(),
            ),
            shippingName = shippingName,
        )
    }
}
