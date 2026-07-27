package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
import com.stripe.android.checkout.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic as PSAutomatic
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full as PSFull

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutEmbeddedConfigurationFactoryTest {

    @Test
    fun `uses the provided merchant display name`() {
        val result = factory(merchantDisplayName = "Acme Corp").create(
            configuration = controllerConfiguration(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.merchantDisplayName).isEqualTo("Acme Corp")
    }

    @Test
    fun `propagates embeddedViewDisplaysMandateText when true`() {
        val result = factory().create(
            configuration = controllerConfiguration(embeddedViewDisplaysMandateText = true),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.embeddedViewDisplaysMandateText).isTrue()
    }

    @Test
    fun `propagates embeddedViewDisplaysMandateText when false`() {
        val result = factory().create(
            configuration = controllerConfiguration(embeddedViewDisplaysMandateText = false),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.embeddedViewDisplaysMandateText).isFalse()
    }

    @Test
    fun `maps billingDetailsCollectionConfiguration address`() {
        val result = factory().create(
            configuration = controllerConfiguration(billingDetailsAddress = Full),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(requiresBillingAddress = false),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(PSFull)
    }

    @Test
    fun `upgrades Automatic to Full when the session requires a billing address`() {
        val result = factory().create(
            configuration = controllerConfiguration(billingDetailsAddress = Automatic),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(requiresBillingAddress = true),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(PSFull)
    }

    @Test
    fun `leaves Automatic unchanged when the session does not require a billing address`() {
        val result = factory().create(
            configuration = controllerConfiguration(billingDetailsAddress = Automatic),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(requiresBillingAddress = false),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(PSAutomatic)
    }

    @Test
    fun `maps googlePayConfiguration using the checkout session country`() {
        val configuration = controllerConfiguration(
            googlePayConfiguration = GooglePayConfiguration(GooglePayConfiguration.Environment.Production)
                .label("Total")
                .buttonType(GooglePayConfiguration.ButtonType.Checkout)
                .additionalEnabledNetworks(listOf("INTERAC")),
        )

        val result = factory().create(
            configuration = configuration,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = "GB"),
            collectedDetails = collectedDetails(),
        )

        val googlePay = requireNotNull(result.googlePay)
        assertThat(googlePay.environment)
            .isEqualTo(PaymentSheet.GooglePayConfiguration.Environment.Production)
        assertThat(googlePay.countryCode).isEqualTo("GB")
        assertThat(googlePay.label).isEqualTo("Total")
        assertThat(googlePay.buttonType)
            .isEqualTo(PaymentSheet.GooglePayConfiguration.ButtonType.Checkout)
        assertThat(googlePay.additionalEnabledNetworks).containsExactly("INTERAC")
    }

    @Test
    fun `leaves googlePay null when the checkout session country is missing`() {
        val configuration = controllerConfiguration(
            googlePayConfiguration = GooglePayConfiguration(GooglePayConfiguration.Environment.Production),
        )

        val result = factory().create(
            configuration = configuration,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = null),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.googlePay).isNull()
    }

    @Test
    fun `leaves googlePay null when the merchant supplied no googlePayConfiguration`() {
        val result = factory().create(
            configuration = controllerConfiguration(googlePayConfiguration = null),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = "US"),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.googlePay).isNull()
    }

    @Test
    fun `sources the billing email from the checkout session customer email`() {
        val result = factory().create(
            configuration = controllerConfiguration(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(customerEmail = "checkout@example.com"),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.defaultBillingDetails?.email).isEqualTo("checkout@example.com")
    }

    @Test
    fun `populates billing details from the session data`() {
        val result = factory().create(
            configuration = controllerConfiguration(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(
                billingName = "Jane Billing",
                billingPhoneNumber = "5559876543",
                billingAddress = Address.State(
                    city = "Denver",
                    country = "US",
                    line1 = "123 Main St",
                    line2 = "Apt 4",
                    postalCode = "80202",
                    state = "CO",
                ),
            ),
        )

        val billingDetails = requireNotNull(result.defaultBillingDetails)
        assertThat(billingDetails.name).isEqualTo("Jane Billing")
        assertThat(billingDetails.phone).isEqualTo("5559876543")
        val address = requireNotNull(billingDetails.address)
        assertThat(address.city).isEqualTo("Denver")
        assertThat(address.country).isEqualTo("US")
        assertThat(address.line1).isEqualTo("123 Main St")
        assertThat(address.line2).isEqualTo("Apt 4")
        assertThat(address.postalCode).isEqualTo("80202")
        assertThat(address.state).isEqualTo("CO")
    }

    @Test
    fun `populates shipping details from the session data`() {
        val result = factory().create(
            configuration = controllerConfiguration(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(
                shippingName = "John Shipping",
                shippingPhoneNumber = "5551234567",
                shippingAddress = Address.State(
                    city = "Denver",
                    country = "US",
                    line1 = "123 Main St",
                    line2 = "Apt 4",
                    postalCode = "80202",
                    state = "CO",
                ),
            ),
        )

        assertThat(result.shippingDetails?.name).isEqualTo("John Shipping")
        assertThat(result.shippingDetails?.phoneNumber).isEqualTo("5551234567")
        val address = requireNotNull(result.shippingDetails?.address)
        assertThat(address.city).isEqualTo("Denver")
        assertThat(address.country).isEqualTo("US")
        assertThat(address.line1).isEqualTo("123 Main St")
        assertThat(address.line2).isEqualTo("Apt 4")
        assertThat(address.postalCode).isEqualTo("80202")
        assertThat(address.state).isEqualTo("CO")
    }

    @Test
    fun `sets attachDefaultsToPaymentMethod to true`() {
        val result = factory().create(
            configuration = controllerConfiguration(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod).isTrue()
    }

    private fun factory(
        merchantDisplayName: String = "Example, Inc.",
    ) = CheckoutEmbeddedConfigurationFactory(merchantDisplayName = merchantDisplayName)

    private fun controllerConfiguration(
        embeddedViewDisplaysMandateText: Boolean = true,
        billingDetailsAddress: BillingDetailsCollectionConfiguration.AddressCollectionMode = Automatic,
        googlePayConfiguration: GooglePayConfiguration? = null,
    ): CheckoutController.Configuration.State {
        val builder = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration()
                    .embeddedViewDisplaysMandateText(embeddedViewDisplaysMandateText)
                    .billingDetailsCollectionConfiguration(
                        BillingDetailsCollectionConfiguration().address(billingDetailsAddress)
                    )
            )
        if (googlePayConfiguration != null) {
            builder.googlePayConfiguration(googlePayConfiguration)
        }
        return builder.build()
    }

    private fun collectedDetails(
        shippingName: String? = null,
        billingName: String? = null,
        shippingPhoneNumber: String? = null,
        billingPhoneNumber: String? = null,
        shippingAddress: Address.State? = null,
        billingAddress: Address.State? = null,
    ): CheckoutCollectedDetails {
        return CheckoutCollectedDetails(
            shippingName = shippingName,
            billingName = billingName,
            shippingPhoneNumber = shippingPhoneNumber,
            billingPhoneNumber = billingPhoneNumber,
            shippingAddress = shippingAddress,
            billingAddress = billingAddress,
        )
    }
}
