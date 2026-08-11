package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Address
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.elements.PaymentElement
import com.stripe.android.elements.PaymentElement.Configuration.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.PaymentElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
import com.stripe.android.elements.PaymentElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic as PSAutomatic
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full as PSFull

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutCommonConfigurationFactoryTest {

    @Test
    fun `matches the common configuration derived from the embedded configuration`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration()
                    .embeddedViewDisplaysMandateText(false)
                    .billingDetailsCollectionConfiguration(
                        BillingDetailsCollectionConfiguration().address(Full)
                    )
            )
            .googlePayConfiguration(
                GooglePayConfiguration(GooglePayConfiguration.Environment.Production)
                    .label("Total")
                    .buttonType(GooglePayConfiguration.ButtonType.Checkout)
                    .additionalEnabledNetworks(listOf("INTERAC"))
            )
            .build()
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            merchantCountry = "GB",
            customerEmail = "checkout@example.com",
            requiresBillingAddress = true,
        )
        val collectedDetails = collectedDetails(
            billingName = "Jane Billing",
            billingPhoneNumber = "5559876543",
            billingAddress = address(),
            shippingName = "John Shipping",
            shippingPhoneNumber = "5551234567",
            shippingAddress = address(),
        )

        val expected = CheckoutEmbeddedConfigurationFactory(appName = "Test App")
            .create(configuration, checkoutSessionResponse, collectedDetails)
            .asCommonConfiguration()
        val actual = factory()
            .create(configuration, checkoutSessionResponse, collectedDetails)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `uses the provided merchant display name`() {
        val configuration = CheckoutController.Configuration()
            .merchantDisplayName("Acme Corp")
            .build()

        val result = factory().create(
            configuration = configuration,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.merchantDisplayName).isEqualTo("Acme Corp")
    }

    @Test
    fun `falls back to the checkout session business name when merchant display name is unset`() {
        val result = factory().create(
            configuration = controllerConfiguration(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(businessName = "Session Biz"),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.merchantDisplayName).isEqualTo("Session Biz")
    }

    @Test
    fun `falls back to the app name when merchant display name and business name are unset`() {
        val result = factory(appName = "My App").create(
            configuration = controllerConfiguration(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(businessName = null),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.merchantDisplayName).isEqualTo("My App")
    }

    @Test
    fun `maps googlePay using the checkout session country`() {
        val result = factory().create(
            configuration = controllerConfiguration(
                googlePayConfiguration = GooglePayConfiguration(GooglePayConfiguration.Environment.Production),
            ),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = "GB"),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.googlePay?.countryCode).isEqualTo("GB")
        assertThat(result.googlePay?.environment)
            .isEqualTo(PaymentSheet.GooglePayConfiguration.Environment.Production)
    }

    @Test
    fun `leaves googlePay null when the checkout session country is missing`() {
        val result = factory().create(
            configuration = controllerConfiguration(
                googlePayConfiguration = GooglePayConfiguration(GooglePayConfiguration.Environment.Production),
            ),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = null),
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
    fun `upgrades billing address collection to Full when the session requires a billing address`() {
        val result = factory().create(
            configuration = controllerConfiguration(billingDetailsAddress = Automatic),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(requiresBillingAddress = true),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(PSFull)
    }

    @Test
    fun `leaves billing address collection Automatic when the session does not require a billing address`() {
        val result = factory().create(
            configuration = controllerConfiguration(billingDetailsAddress = Automatic),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(requiresBillingAddress = false),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(PSAutomatic)
    }

    @Test
    fun `applies configuration defaults for fields checkout does not configure`() {
        val result = factory().create(
            configuration = controllerConfiguration(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.customer).isEqualTo(ConfigurationDefaults.customer)
        assertThat(result.cardBrandAcceptance).isEqualTo(ConfigurationDefaults.cardBrandAcceptance)
        assertThat(result.customPaymentMethods).isEqualTo(ConfigurationDefaults.customPaymentMethods)
        assertThat(result.walletButtons).isNull()
        assertThat(result.googlePlacesApiKey).isNull()
    }

    private fun factory(appName: String = "Test App") = CheckoutCommonConfigurationFactory(appName)

    private fun controllerConfiguration(
        billingDetailsAddress: BillingDetailsCollectionConfiguration.AddressCollectionMode = Automatic,
        googlePayConfiguration: GooglePayConfiguration? = null,
    ): CheckoutController.Configuration.State {
        val builder = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration()
                    .billingDetailsCollectionConfiguration(
                        BillingDetailsCollectionConfiguration().address(billingDetailsAddress)
                    )
            )
        if (googlePayConfiguration != null) {
            builder.googlePayConfiguration(googlePayConfiguration)
        }
        return builder.build()
    }

    private fun address() = Address.State(
        city = "Denver",
        country = "US",
        line1 = "123 Main St",
        line2 = "Apt 4",
        postalCode = "80202",
        state = "CO",
    )

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
