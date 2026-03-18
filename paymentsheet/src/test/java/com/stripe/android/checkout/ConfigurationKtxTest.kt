package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
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
    fun `sets billing name from state when defaultBillingDetails name is null`() {
        val config = configuration()
        val state = state(billingName = "Jane Billing")

        val result = config.forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.name).isEqualTo("Jane Billing")
    }

    @Test
    fun `preserves existing billing details fields when setting billing name`() {
        val address = PaymentSheet.Address(
            city = "San Francisco",
            country = "US",
        )
        val config = configuration(
            defaultBillingDetails = PaymentSheet.BillingDetails(
                address = address,
                email = "existing@example.com",
                phone = "5551234567",
            ),
        )
        val state = state(billingName = "Jane Billing")

        val result = config.forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.name).isEqualTo("Jane Billing")
        assertThat(result.defaultBillingDetails?.email).isEqualTo("existing@example.com")
        assertThat(result.defaultBillingDetails?.phone).isEqualTo("5551234567")
        assertThat(result.defaultBillingDetails?.address).isEqualTo(address)
    }

    @Test
    fun `preserves merchant shipping name when already set`() {
        val config = configuration(
            shippingDetails = AddressDetails(name = "Merchant Shipping"),
        )
        val state = state(shippingName = "State Shipping")

        val result = config.forCheckoutSession(state)

        assertThat(result.shippingDetails?.name).isEqualTo("Merchant Shipping")
    }

    @Test
    fun `preserves merchant billing name when already set`() {
        val config = configuration(
            defaultBillingDetails = PaymentSheet.BillingDetails(name = "Merchant Billing"),
        )
        val state = state(billingName = "State Billing")

        val result = config.forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.name).isEqualTo("Merchant Billing")
    }

    @Test
    fun `sets email and billing name and shipping name simultaneously`() {
        val config = configuration()
        val state = state(
            customerEmail = "test@example.com",
            billingName = "Jane Billing",
            shippingName = "John Shipping",
        )

        val result = config.forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("test@example.com")
        assertThat(result.defaultBillingDetails?.name).isEqualTo("Jane Billing")
        assertThat(result.shippingDetails?.name).isEqualTo("John Shipping")
    }

    @Test
    fun `preserves other configuration properties`() {
        val config = PaymentSheet.Configuration.Builder("Test Merchant")
            .primaryButtonLabel("Pay now")
            .build()
        val state = state()

        val result = config.forCheckoutSession(state)

        assertThat(result.merchantDisplayName).isEqualTo("Test Merchant")
        assertThat(result.primaryButtonLabel).isEqualTo("Pay now")
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

    @Test
    fun `sets billing address from state when defaultBillingDetails address is null`() {
        val config = configuration()
        val state = state(
            billingAddress = Address.State(
                city = "Denver",
                country = "US",
                line1 = "123 Main St",
                line2 = "Apt 4",
                postalCode = "80202",
                state = "CO",
            ),
        )

        val result = config.forCheckoutSession(state)

        val address = result.defaultBillingDetails?.address
        assertThat(address).isNotNull()
        assertThat(address!!.city).isEqualTo("Denver")
        assertThat(address.country).isEqualTo("US")
        assertThat(address.line1).isEqualTo("123 Main St")
        assertThat(address.line2).isEqualTo("Apt 4")
        assertThat(address.postalCode).isEqualTo("80202")
        assertThat(address.state).isEqualTo("CO")
    }

    @Test
    fun `preserves merchant billing address when already set`() {
        val merchantAddress = PaymentSheet.Address(
            city = "San Francisco",
            country = "US",
        )
        val config = configuration(
            defaultBillingDetails = PaymentSheet.BillingDetails(address = merchantAddress),
        )
        val state = state(
            billingAddress = Address.State(
                city = "Denver",
                country = "US",
                line1 = null,
                line2 = null,
                postalCode = null,
                state = null,
            ),
        )

        val result = config.forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.address).isEqualTo(merchantAddress)
    }

    @Test
    fun `sets shipping address from state when shippingDetails address is null`() {
        val config = configuration()
        val state = state(
            shippingAddress = Address.State(
                city = "Denver",
                country = "US",
                line1 = "123 Main St",
                line2 = "Apt 4",
                postalCode = "80202",
                state = "CO",
            ),
        )

        val result = config.forCheckoutSession(state)

        val address = result.shippingDetails?.address
        assertThat(address).isNotNull()
        assertThat(address!!.city).isEqualTo("Denver")
        assertThat(address.country).isEqualTo("US")
        assertThat(address.line1).isEqualTo("123 Main St")
        assertThat(address.line2).isEqualTo("Apt 4")
        assertThat(address.postalCode).isEqualTo("80202")
        assertThat(address.state).isEqualTo("CO")
    }

    @Test
    fun `preserves merchant shipping address when already set`() {
        val merchantAddress = PaymentSheet.Address(
            city = "San Francisco",
            country = "US",
        )
        val config = configuration(
            shippingDetails = AddressDetails(address = merchantAddress),
        )
        val state = state(
            shippingAddress = Address.State(
                city = "Denver",
                country = "US",
                line1 = null,
                line2 = null,
                postalCode = null,
                state = null,
            ),
        )

        val result = config.forCheckoutSession(state)

        assertThat(result.shippingDetails?.address).isEqualTo(merchantAddress)
    }

    private fun state(
        customerEmail: String? = null,
        shippingName: String? = null,
        billingName: String? = null,
        shippingAddress: Address.State? = null,
        billingAddress: Address.State? = null,
    ): InternalState {
        return InternalState(
            key = "ConfigurationKtxTest",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                customerEmail = customerEmail,
            ),
            shippingName = shippingName,
            billingName = billingName,
            shippingAddress = shippingAddress,
            billingAddress = billingAddress,
        )
    }
}
