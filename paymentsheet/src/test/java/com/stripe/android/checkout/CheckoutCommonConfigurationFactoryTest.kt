package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Address
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.GooglePayConfiguration
import com.stripe.android.elements.PaymentElement
import com.stripe.android.elements.PaymentElement.Configuration.Appearance
import com.stripe.android.elements.PaymentElement.Configuration.TermsDisplay
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic as PSAutomatic
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full as PSFull

@OptIn(
    CheckoutSessionPreview::class,
    com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class,
    CardFundingFilteringPrivatePreview::class,
)
internal class CheckoutCommonConfigurationFactoryTest {

    @Test
    fun `create uses explicit payment element configuration`() {
        val paymentElementConfiguration = PaymentElement.Configuration()
            .preferredNetworks(listOf(CardBrand.CartesBancaires))
            .paymentMethodOrder(listOf("klarna", "card"))
            .opensCardScannerAutomatically(true)
            .cardBrandAcceptance(
                PaymentElement.Configuration.CardBrandAcceptance.disallowed(
                    listOf(PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Amex)
                )
            )
            .termsDisplay(mapOf(PaymentMethod.Type.Card to TermsDisplay.NEVER))
            .appearance(Appearance().shapes(Appearance.Shapes().cornerRadiusDp(12f)))
            .googlePayConfiguration(PaymentElement.Configuration.GooglePayConfiguration().label("Explicit total"))
            .linkConfiguration(
                PaymentElement.Configuration.LinkConfiguration()
                    .display(PaymentElement.Configuration.LinkConfiguration.Display.Never)
            )
            .build()

        val result = factory().create(
            configuration = CheckoutController.Configuration().build(),
            paymentElementConfiguration = paymentElementConfiguration,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = "US"),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.preferredNetworks).containsExactly(CardBrand.CartesBancaires)
        assertThat(result.paymentMethodOrder).containsExactly("klarna", "card").inOrder()
        assertThat(result.opensCardScannerAutomatically).isTrue()
        assertThat(result.cardBrandAcceptance).isEqualTo(
            PaymentSheet.CardBrandAcceptance.disallowed(
                listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Amex)
            )
        )
        assertThat(result.termsDisplay).containsExactly(PaymentMethod.Type.Card, PaymentSheet.TermsDisplay.NEVER)
        assertThat(result.appearance.shapes.cornerRadiusDp).isEqualTo(12f)
        assertThat(result.googlePay?.label).isEqualTo("Explicit total")
        assertThat(result.link.display).isEqualTo(PaymentSheet.LinkConfiguration.Display.Never)
    }

    @Test
    fun `createForPaymentElement returns null when payment element is not configured`() {
        val result = CheckoutCommonConfigurationFactory(appName = "Test App").createForPaymentElement(
            configuration = CheckoutController.Configuration().build(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `matches the common configuration derived from the embedded configuration`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration()
                    .embeddedViewDisplaysMandateText(false)
            )
            .expressCheckoutElement(
                ExpressCheckoutElement.Configuration().googlePayConfiguration(
                    GooglePayConfiguration()
                        .label("Total")
                        .buttonType(GooglePayConfiguration.ButtonType.Checkout)
                        .additionalEnabledNetworks(listOf("INTERAC"))
                )
            )
            .build()
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            merchantCountry = "GB",
            customerEmail = "checkout@example.com",
            requiresBillingAddress = true,
        )
        val collectedDetails = collectedDetails(
            shippingName = "John Shipping",
            shippingAddress = address(),
        )

        val expected = CheckoutEmbeddedConfigurationFactory(appName = "Test App")
            .create(
                configuration = configuration,
                paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
                checkoutSessionResponse = checkoutSessionResponse,
                collectedDetails = collectedDetails,
            )
            .asCommonConfiguration()
        val actual = factory()
            .create(
                configuration = configuration,
                paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
                checkoutSessionResponse = checkoutSessionResponse,
                collectedDetails = collectedDetails,
            )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `uses the provided merchant display name`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(PaymentElement.Configuration())
            .merchantDisplayName("Acme Corp")
            .build()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.merchantDisplayName).isEqualTo("Acme Corp")
    }

    @Test
    fun `falls back to the checkout session business name when merchant display name is unset`() {
        val configuration = controllerConfiguration()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(businessName = "Session Biz"),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.merchantDisplayName).isEqualTo("Session Biz")
    }

    @Test
    fun `falls back to the app name when merchant display name and business name are unset`() {
        val configuration = controllerConfiguration()

        val result = factory(appName = "My App").create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(businessName = null),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.merchantDisplayName).isEqualTo("My App")
    }

    @Test
    fun `maps googlePay using the checkout session country`() {
        val configuration = controllerConfiguration()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                merchantCountry = "GB",
                liveMode = true,
            ),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.googlePay?.countryCode).isEqualTo("GB")
        assertThat(result.googlePay?.environment)
            .isEqualTo(PaymentSheet.GooglePayConfiguration.Environment.Test)
    }

    @Test
    fun `leaves googlePay null when the checkout session country is missing`() {
        val configuration = controllerConfiguration(
            googlePayConfiguration = GooglePayConfiguration(),
        )

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = null),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.googlePay).isNull()
    }

    @Test
    fun `maps Link configuration from payment element configuration`() {
        val configuration = CheckoutController.Configuration().paymentElement(
            PaymentElement.Configuration().linkConfiguration(
                PaymentElement.Configuration.LinkConfiguration().display(
                    PaymentElement.Configuration.LinkConfiguration.Display.Never
                )
            )
        ).build()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.link.display).isEqualTo(PaymentSheet.LinkConfiguration.Display.Never)
    }

    @Test
    fun `createForPaymentElement uses payment element configuration`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration()
                    .googlePayConfiguration(
                        PaymentElement.Configuration.GooglePayConfiguration()
                            .label("PE total")
                            .buttonType(PaymentElement.Configuration.GooglePayConfiguration.ButtonType.Buy)
                    )
                    .linkConfiguration(
                        PaymentElement.Configuration.LinkConfiguration().display(
                            PaymentElement.Configuration.LinkConfiguration.Display.Never
                        )
                    )
            )
            .expressCheckoutElement(
                ExpressCheckoutElement.Configuration()
                    .googlePayConfiguration(
                        GooglePayConfiguration()
                            .label("ECE total")
                            .buttonType(GooglePayConfiguration.ButtonType.Donate)
                    )
                    .linkConfiguration(
                        ExpressCheckoutElement.Configuration.LinkConfiguration().display(
                            ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic
                        )
                    )
            )
            .build()
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = "US")
        val collectedDetails = collectedDetails()

        val result = factory().createForPaymentElement(
            configuration = configuration,
            checkoutSessionResponse = checkoutSessionResponse,
            collectedDetails = collectedDetails,
        )
        assertThat(result?.googlePay?.label).isEqualTo("PE total")
        assertThat(result?.googlePay?.buttonType).isEqualTo(PaymentSheet.GooglePayConfiguration.ButtonType.Buy)
        assertThat(result?.link?.display).isEqualTo(PaymentSheet.LinkConfiguration.Display.Never)
        assertThat(result?.billingDetailsCollectionConfiguration?.address).isEqualTo(PSAutomatic)
    }

    @Test
    fun `maps configured billing defaults to payment element and express checkout element`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(PaymentElement.Configuration())
            .defaults(
                CheckoutController.Configuration.Defaults().phone("+15555551234").billingDetails(
                    CheckoutController.Configuration.Defaults.ContactDetails()
                        .name("Jane Billing")
                        .address(
                            CheckoutController.Address()
                                .city("Denver")
                                .country("US")
                                .line1("123 Main St")
                                .line2("Apt 4")
                                .postalCode("80202")
                                .state("CO"),
                        ),
                ),
            )
            .expressCheckoutElement(ExpressCheckoutElement.Configuration())
            .build()
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(customerEmail = "checkout@example.com")
        val collectedDetails = collectedDetails()

        val paymentElementDefaults = factory().createForPaymentElement(
            configuration = configuration,
            checkoutSessionResponse = checkoutSessionResponse,
            collectedDetails = collectedDetails,
        )?.defaultBillingDetails
        val expressCheckoutElementDefaults = requireNotNull(
            factory().createForExpressCheckoutElement(
                configuration = configuration,
                checkoutSessionResponse = checkoutSessionResponse,
                collectedDetails = collectedDetails,
            ),
        ).defaultBillingDetails

        assertThat(expressCheckoutElementDefaults).isEqualTo(paymentElementDefaults)
        assertThat(paymentElementDefaults?.email).isEqualTo("checkout@example.com")
        assertThat(paymentElementDefaults?.name).isEqualTo("Jane Billing")
        assertThat(paymentElementDefaults?.phone).isEqualTo("+15555551234")
        assertThat(paymentElementDefaults?.address?.city).isEqualTo("Denver")
        assertThat(paymentElementDefaults?.address?.country).isEqualTo("US")
        assertThat(paymentElementDefaults?.address?.line1).isEqualTo("123 Main St")
        assertThat(paymentElementDefaults?.address?.line2).isEqualTo("Apt 4")
        assertThat(paymentElementDefaults?.address?.postalCode).isEqualTo("80202")
        assertThat(paymentElementDefaults?.address?.state).isEqualTo("CO")
    }

    @Test
    fun `maps terms display to common configuration`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration().termsDisplay(
                    mapOf(PaymentMethod.Type.Card to TermsDisplay.NEVER)
                )
            )
            .build()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.termsDisplay)
            .isEqualTo(mapOf(PaymentMethod.Type.Card to PaymentSheet.TermsDisplay.NEVER))
    }

    @Test
    fun `maps payment method order to common configuration`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(PaymentElement.Configuration().paymentMethodOrder(listOf("card", "klarna")))
            .build()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.paymentMethodOrder).isEqualTo(listOf("card", "klarna"))
    }

    @Test
    fun `maps preferred networks to common configuration`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration().preferredNetworks(
                    listOf(CardBrand.CartesBancaires, CardBrand.Visa)
                )
            )
            .build()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.preferredNetworks)
            .isEqualTo(listOf(CardBrand.CartesBancaires, CardBrand.Visa))
    }

    @Test
    fun `maps opens card scanner automatically to common configuration`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(PaymentElement.Configuration().opensCardScannerAutomatically(true))
            .build()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.opensCardScannerAutomatically).isTrue()
    }

    @Test
    fun `maps card brand acceptance to common configuration`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration().cardBrandAcceptance(
                    PaymentElement.Configuration.CardBrandAcceptance.disallowed(
                        listOf(PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Amex)
                    )
                )
            )
            .build()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.cardBrandAcceptance).isEqualTo(
            PaymentSheet.CardBrandAcceptance.disallowed(
                listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Amex)
            )
        )
    }

    @Test
    fun `sources the collected email over the checkout session customer email`() {
        val configuration = controllerConfiguration()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(customerEmail = "checkout@example.com"),
            collectedDetails = collectedDetails(email = "collected@example.com"),
        )

        assertThat(result.defaultBillingDetails?.email).isEqualTo("collected@example.com")
    }

    @Test
    fun `upgrades billing address collection to Full when the session requires a billing address`() {
        val configuration = controllerConfiguration()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(requiresBillingAddress = true),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(PSFull)
    }

    @Test
    fun `leaves billing address collection Automatic when the session does not require a billing address`() {
        val configuration = controllerConfiguration()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(requiresBillingAddress = false),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(PSAutomatic)
    }

    @Test
    fun `applies configuration defaults for fields checkout does not configure`() {
        val configuration = controllerConfiguration()

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.customer).isEqualTo(ConfigurationDefaults.customer)
        assertThat(result.cardBrandAcceptance).isEqualTo(ConfigurationDefaults.cardBrandAcceptance)
        assertThat(result.customPaymentMethods).isEqualTo(ConfigurationDefaults.customPaymentMethods)
        assertThat(result.walletButtons).isNull()
        assertThat(result.googlePlacesApiKey).isNull()
    }

    @Test
    fun `propagates payment element appearance`() {
        val configuration = controllerConfiguration(
            appearance = PaymentElement.Configuration.Appearance()
                .colorsLight(
                    PaymentElement.Configuration.Appearance.Colors.light().primary(0xFF123456.toInt())
                )
                .typography(Appearance.Typography().sizeScaleFactor(1.5f))
                .shapes(Appearance.Shapes().cornerRadiusDp(12f))
                .embeddedAppearance(
                    Appearance.Embedded()
                        .rowStyle(Appearance.Embedded.RowStyle.FloatingButton().spacingDp(7f))
                )
        )

        val result = factory().create(
            configuration = configuration,
            paymentElementConfiguration = requireNotNull(configuration.paymentElementConfiguration),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result.appearance.colorsLight.primary).isEqualTo(0xFF123456.toInt())
        assertThat(result.appearance.typography.sizeScaleFactor).isEqualTo(1.5f)
        assertThat(result.appearance.shapes.cornerRadiusDp).isEqualTo(12f)
        assertThat(result.appearance.embeddedAppearance.style).isEqualTo(
            PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton.Builder().spacingDp(7f).build()
        )
    }

    @Test
    fun `clearing default phone leaves billing phone null`() {
        val defaults = CheckoutController.Configuration.Defaults().phone("+15555551234")
        assertThat(defaults.build().phone).isEqualTo("+15555551234")
        val configuration = CheckoutController.Configuration()
            .paymentElement(PaymentElement.Configuration())
            .defaults(defaults.phone(null))
            .expressCheckoutElement(ExpressCheckoutElement.Configuration())
            .build()
        val response = CheckoutSessionResponseFactory.create()
        val details = collectedDetails()

        val paymentElementConfiguration = requireNotNull(
            factory().createForPaymentElement(configuration, response, details)
        )
        assertThat(paymentElementConfiguration.defaultBillingDetails?.phone).isNull()
        val expressConfiguration = requireNotNull(
            factory().createForExpressCheckoutElement(configuration, response, details)
        )
        assertThat(expressConfiguration.defaultBillingDetails?.phone).isNull()
    }

    private fun factory(appName: String = "Test App") = CheckoutCommonConfigurationFactory(appName)

    private fun controllerConfiguration(
        appearance: PaymentElement.Configuration.Appearance = PaymentElement.Configuration.Appearance(),
        googlePayConfiguration: GooglePayConfiguration? = null,
    ): CheckoutController.Configuration.State {
        val builder = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration()
                    .appearance(appearance)
            )
        val expressCheckoutElementConfiguration = ExpressCheckoutElement.Configuration()
        if (googlePayConfiguration != null) {
            expressCheckoutElementConfiguration.googlePayConfiguration(googlePayConfiguration)
        }
        builder.expressCheckoutElement(expressCheckoutElementConfiguration)
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
        email: String? = null,
        shippingName: String? = null,
        shippingAddress: Address.State? = null,
    ): CheckoutCollectedDetails {
        return CheckoutCollectedDetails(
            email = email,
            shippingName = shippingName,
            shippingAddress = shippingAddress,
        )
    }
}
