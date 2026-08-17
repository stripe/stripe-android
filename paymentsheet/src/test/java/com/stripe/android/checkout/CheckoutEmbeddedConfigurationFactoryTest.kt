package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Address
import com.stripe.android.elements.PaymentElement
import com.stripe.android.elements.PaymentElement.Configuration.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.PaymentElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
import com.stripe.android.elements.PaymentElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
import com.stripe.android.elements.PaymentElement.Configuration.TermsDisplay
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic as PSAutomatic
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full as PSFull

@OptIn(
    CheckoutSessionPreview::class,
    com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class,
)
internal class CheckoutEmbeddedConfigurationFactoryTest {

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
    fun `propagates payment element appearance`() {
        val result = factory().create(
            configuration = controllerConfiguration(
                appearance = PaymentElement.Configuration.Appearance()
                    .colorsLight(
                        PaymentElement.Configuration.Appearance.Colors.light().primary(0xFF123456.toInt())
                    )
            ),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.appearance.colorsLight.primary).isEqualTo(0xFF123456.toInt())
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
    fun `maps preferred networks to embedded configuration`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration().preferredNetworks(
                    listOf(CardBrand.CartesBancaires, CardBrand.Visa)
                )
            )
            .build()

        val result = factory().create(
            configuration = configuration,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.preferredNetworks)
            .isEqualTo(listOf(CardBrand.CartesBancaires, CardBrand.Visa))
    }

    @Test
    fun `maps opens card scanner automatically to embedded configuration`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(PaymentElement.Configuration().opensCardScannerAutomatically(true))
            .build()

        val result = factory().create(
            configuration = configuration,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.opensCardScannerAutomatically).isTrue()
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
    fun `maps terms display to embedded payment element configuration`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration().termsDisplay(
                    mapOf(PaymentMethod.Type.Card to TermsDisplay.NEVER)
                )
            )
            .build()

        val result = factory().create(
            configuration = configuration,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.termsDisplay)
            .isEqualTo(mapOf(PaymentMethod.Type.Card to PaymentSheet.TermsDisplay.NEVER))
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

    private fun factory(appName: String = "Test App") = CheckoutEmbeddedConfigurationFactory(appName)

    private fun controllerConfiguration(
        embeddedViewDisplaysMandateText: Boolean = true,
        appearance: PaymentElement.Configuration.Appearance = PaymentElement.Configuration.Appearance(),
        billingDetailsAddress: BillingDetailsCollectionConfiguration.AddressCollectionMode = Automatic,
        googlePayConfiguration: GooglePayConfiguration? = null,
    ): CheckoutController.Configuration.State {
        val builder = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration()
                    .embeddedViewDisplaysMandateText(embeddedViewDisplaysMandateText)
                    .appearance(appearance)
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
        shippingAddress: Address.State? = null,
        billingAddress: Address.State? = null,
    ): CheckoutCollectedDetails {
        return CheckoutCollectedDetails(
            shippingName = shippingName,
            billingName = billingName,
            shippingAddress = shippingAddress,
            billingAddress = billingAddress,
        )
    }
}
