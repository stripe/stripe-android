package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
class CheckoutConfigurationMergerTest {

    @Test
    fun `embedded - sets email from checkout session when defaultBillingDetails is null`() {
        val config = embeddedConfiguration()
        val state = state(customerEmail = "test@example.com")

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("test@example.com")
    }

    @Test
    fun `embedded - preserves merchant email when already set`() {
        val config = embeddedConfiguration(
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "merchant@example.com"),
        )
        val state = state(customerEmail = "checkout@example.com")

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("merchant@example.com")
    }

    @Test
    fun `embedded - sets billing name from state when defaultBillingDetails name is null`() {
        val config = embeddedConfiguration()
        val state = state(billingName = "Jane Billing")

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.name).isEqualTo("Jane Billing")
    }

    @Test
    fun `embedded - preserves merchant billing name when already set`() {
        val config = embeddedConfiguration(
            defaultBillingDetails = PaymentSheet.BillingDetails(name = "Merchant Billing"),
        )
        val state = state(billingName = "State Billing")

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.name).isEqualTo("Merchant Billing")
    }

    @Test
    fun `embedded - sets billing phone from state when defaultBillingDetails is null`() {
        val config = embeddedConfiguration()
        val state = state(billingPhoneNumber = "5559876543")

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.phone).isEqualTo("5559876543")
    }

    @Test
    fun `embedded - preserves merchant billing phone when already set`() {
        val config = embeddedConfiguration(
            defaultBillingDetails = PaymentSheet.BillingDetails(phone = "5550000000"),
        )
        val state = state(billingPhoneNumber = "5559876543")

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.phone).isEqualTo("5550000000")
    }

    @Test
    fun `embedded - sets billing address from state when defaultBillingDetails address is null`() {
        val config = embeddedConfiguration()
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

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

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
    fun `embedded - preserves merchant billing address when already set`() {
        val merchantAddress = PaymentSheet.Address(
            city = "San Francisco",
            country = "US",
        )
        val config = embeddedConfiguration(
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

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.address).isEqualTo(merchantAddress)
    }

    @Test
    fun `embedded - sets shipping name from state when shippingDetails is null`() {
        val config = embeddedConfiguration()
        val state = state(shippingName = "John Doe")

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.shippingDetails?.name).isEqualTo("John Doe")
    }

    @Test
    fun `embedded - preserves merchant shipping name when already set`() {
        val config = embeddedConfiguration(
            shippingDetails = AddressDetails(name = "Merchant Shipping"),
        )
        val state = state(shippingName = "State Shipping")

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.shippingDetails?.name).isEqualTo("Merchant Shipping")
    }

    @Test
    fun `embedded - sets shipping phoneNumber from state when shippingDetails is null`() {
        val config = embeddedConfiguration()
        val state = state(shippingPhoneNumber = "5551234567")

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.shippingDetails?.phoneNumber).isEqualTo("5551234567")
    }

    @Test
    fun `embedded - preserves merchant shipping phoneNumber when already set`() {
        val config = embeddedConfiguration(
            shippingDetails = AddressDetails(phoneNumber = "5550000000"),
        )
        val state = state(shippingPhoneNumber = "5551234567")

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.shippingDetails?.phoneNumber).isEqualTo("5550000000")
    }

    @Test
    fun `embedded - sets shipping address from state when shippingDetails address is null`() {
        val config = embeddedConfiguration()
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

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

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
    fun `embedded - preserves merchant shipping address when already set`() {
        val merchantAddress = PaymentSheet.Address(
            city = "San Francisco",
            country = "US",
        )
        val config = embeddedConfiguration(
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

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.shippingDetails?.address).isEqualTo(merchantAddress)
    }

    @Test
    fun `embedded - sets email and billing name and shipping name simultaneously`() {
        val config = embeddedConfiguration()
        val state = state(
            customerEmail = "test@example.com",
            billingName = "Jane Billing",
            shippingName = "John Shipping",
        )

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("test@example.com")
        assertThat(result.defaultBillingDetails?.name).isEqualTo("Jane Billing")
        assertThat(result.shippingDetails?.name).isEqualTo("John Shipping")
    }

    @Test
    fun `embedded - preserves non-billing and non-shipping configuration properties`() {
        val config = EmbeddedPaymentElement.Configuration.Builder("Test Merchant")
            .primaryButtonLabel("Pay now")
            .build()
        val state = state()

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.merchantDisplayName).isEqualTo("Test Merchant")
        assertThat(result.primaryButtonLabel).isEqualTo("Pay now")
    }

    @Test
    fun `paymentSheet - sets email from checkout session when defaultBillingDetails is null`() {
        val config = paymentSheetConfiguration()
        val state = state(customerEmail = "test@example.com")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("test@example.com")
    }

    @Test
    fun `paymentSheet - preserves merchant email when already set`() {
        val config = paymentSheetConfiguration(
            defaultBillingDetails = PaymentSheet.BillingDetails(email = "merchant@example.com"),
        )
        val state = state(customerEmail = "checkout@example.com")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("merchant@example.com")
    }

    @Test
    fun `paymentSheet - preserves existing billing details fields when setting email`() {
        val address = PaymentSheet.Address(
            city = "San Francisco",
            country = "US",
        )
        val config = paymentSheetConfiguration(
            defaultBillingDetails = PaymentSheet.BillingDetails(
                address = address,
                name = "Jane Doe",
                phone = "5551234567",
            ),
        )
        val state = state(customerEmail = "test@example.com")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("test@example.com")
        assertThat(result.defaultBillingDetails?.name).isEqualTo("Jane Doe")
        assertThat(result.defaultBillingDetails?.phone).isEqualTo("5551234567")
        assertThat(result.defaultBillingDetails?.address).isEqualTo(address)
    }

    @Test
    fun `paymentSheet - sets billing name from state when defaultBillingDetails name is null`() {
        val config = paymentSheetConfiguration()
        val state = state(billingName = "Jane Billing")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.name).isEqualTo("Jane Billing")
    }

    @Test
    fun `paymentSheet - preserves merchant billing name when already set`() {
        val config = paymentSheetConfiguration(
            defaultBillingDetails = PaymentSheet.BillingDetails(name = "Merchant Billing"),
        )
        val state = state(billingName = "State Billing")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.name).isEqualTo("Merchant Billing")
    }

    @Test
    fun `paymentSheet - preserves existing billing details fields when setting billing name`() {
        val address = PaymentSheet.Address(
            city = "San Francisco",
            country = "US",
        )
        val config = paymentSheetConfiguration(
            defaultBillingDetails = PaymentSheet.BillingDetails(
                address = address,
                email = "existing@example.com",
                phone = "5551234567",
            ),
        )
        val state = state(billingName = "Jane Billing")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.name).isEqualTo("Jane Billing")
        assertThat(result.defaultBillingDetails?.email).isEqualTo("existing@example.com")
        assertThat(result.defaultBillingDetails?.phone).isEqualTo("5551234567")
        assertThat(result.defaultBillingDetails?.address).isEqualTo(address)
    }

    @Test
    fun `paymentSheet - sets billing phone from state when defaultBillingDetails is null`() {
        val config = paymentSheetConfiguration()
        val state = state(billingPhoneNumber = "5559876543")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.phone).isEqualTo("5559876543")
    }

    @Test
    fun `paymentSheet - preserves merchant billing phone when already set`() {
        val config = paymentSheetConfiguration(
            defaultBillingDetails = PaymentSheet.BillingDetails(phone = "5550000000"),
        )
        val state = state(billingPhoneNumber = "5559876543")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.phone).isEqualTo("5550000000")
    }

    @Test
    fun `paymentSheet - sets billing address from state when defaultBillingDetails address is null`() {
        val config = paymentSheetConfiguration()
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

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

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
    fun `paymentSheet - preserves merchant billing address when already set`() {
        val merchantAddress = PaymentSheet.Address(
            city = "San Francisco",
            country = "US",
        )
        val config = paymentSheetConfiguration(
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

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.address).isEqualTo(merchantAddress)
    }

    @Test
    fun `paymentSheet - sets shipping name from state when shippingDetails is null`() {
        val config = paymentSheetConfiguration()
        val state = state(shippingName = "John Doe")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.shippingDetails?.name).isEqualTo("John Doe")
    }

    @Test
    fun `paymentSheet - preserves merchant shipping name when already set`() {
        val config = paymentSheetConfiguration(
            shippingDetails = AddressDetails(name = "Merchant Shipping"),
        )
        val state = state(shippingName = "State Shipping")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.shippingDetails?.name).isEqualTo("Merchant Shipping")
    }

    @Test
    fun `paymentSheet - preserves existing shipping details fields when setting name`() {
        val address = PaymentSheet.Address(
            city = "San Francisco",
            country = "US",
        )
        val config = paymentSheetConfiguration(
            shippingDetails = AddressDetails(
                address = address,
                phoneNumber = "5551234567",
                isCheckboxSelected = true,
            ),
        )
        val state = state(shippingName = "John Doe")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.shippingDetails?.name).isEqualTo("John Doe")
        assertThat(result.shippingDetails?.address).isEqualTo(address)
        assertThat(result.shippingDetails?.phoneNumber).isEqualTo("5551234567")
        assertThat(result.shippingDetails?.isCheckboxSelected).isTrue()
    }

    @Test
    fun `paymentSheet - sets shipping phoneNumber from state when shippingDetails is null`() {
        val config = paymentSheetConfiguration()
        val state = state(shippingPhoneNumber = "5551234567")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.shippingDetails?.phoneNumber).isEqualTo("5551234567")
    }

    @Test
    fun `paymentSheet - preserves merchant shipping phoneNumber when already set`() {
        val config = paymentSheetConfiguration(
            shippingDetails = AddressDetails(phoneNumber = "5550000000"),
        )
        val state = state(shippingPhoneNumber = "5551234567")

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.shippingDetails?.phoneNumber).isEqualTo("5550000000")
    }

    @Test
    fun `paymentSheet - sets shipping address from state when shippingDetails address is null`() {
        val config = paymentSheetConfiguration()
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

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

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
    fun `paymentSheet - preserves merchant shipping address when already set`() {
        val merchantAddress = PaymentSheet.Address(
            city = "San Francisco",
            country = "US",
        )
        val config = paymentSheetConfiguration(
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

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.shippingDetails?.address).isEqualTo(merchantAddress)
    }

    @Test
    fun `paymentSheet - sets email and billing name and shipping name simultaneously`() {
        val config = paymentSheetConfiguration()
        val state = state(
            customerEmail = "test@example.com",
            billingName = "Jane Billing",
            shippingName = "John Shipping",
        )

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.defaultBillingDetails?.email).isEqualTo("test@example.com")
        assertThat(result.defaultBillingDetails?.name).isEqualTo("Jane Billing")
        assertThat(result.shippingDetails?.name).isEqualTo("John Shipping")
    }

    @Test
    fun `paymentSheet - preserves other configuration properties`() {
        val config = PaymentSheet.Configuration.Builder("Test Merchant")
            .primaryButtonLabel("Pay now")
            .build()
        val state = state()

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.merchantDisplayName).isEqualTo("Test Merchant")
        assertThat(result.primaryButtonLabel).isEqualTo("Pay now")
    }

    @Test
    fun `paymentSheet - sets attachDefaultsToPaymentMethod to true`() {
        val config = paymentSheetConfiguration()
        val state = state()

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod).isTrue()
    }

    @Test
    fun `embedded - sets attachDefaultsToPaymentMethod to true`() {
        val config = embeddedConfiguration()
        val state = state()

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod).isTrue()
    }

    private fun embeddedConfiguration(
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
        shippingDetails: AddressDetails? = null,
    ): EmbeddedPaymentElement.Configuration {
        return EmbeddedPaymentElement.Configuration.Builder("Test Merchant")
            .defaultBillingDetails(defaultBillingDetails)
            .shippingDetails(shippingDetails)
            .build()
    }

    private fun paymentSheetConfiguration(
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
        billingName: String? = null,
        shippingPhoneNumber: String? = null,
        billingPhoneNumber: String? = null,
        shippingAddress: Address.State? = null,
        billingAddress: Address.State? = null,
    ): InternalState {
        return InternalState(
            key = "CheckoutConfigurationMergerTest",
            configuration = Checkout.Configuration().build(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                customerEmail = customerEmail,
            ),
            shippingName = shippingName,
            billingName = billingName,
            shippingPhoneNumber = shippingPhoneNumber,
            billingPhoneNumber = billingPhoneNumber,
            shippingAddress = shippingAddress,
            billingAddress = billingAddress,
        )
    }
}
