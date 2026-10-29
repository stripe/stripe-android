package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.configuration.ConfigurationDefaults
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
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full as PSFull

@OptIn(
    CheckoutSessionPreview::class,
    com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class,
    CardFundingFilteringPrivatePreview::class,
)
internal class CheckoutCommonConfigurationFactoryExpressCheckoutElementTest {

    @Test
    fun `createForExpressCheckoutElement maps Link configuration from express checkout element configuration`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration().linkConfiguration(
                    PaymentElement.Configuration.LinkConfiguration().display(
                        PaymentElement.Configuration.LinkConfiguration.Display.Never
                    )
                )
            )
            .expressCheckoutElement(
                ExpressCheckoutElement.Configuration().linkConfiguration(
                    ExpressCheckoutElement.Configuration.LinkConfiguration().display(
                        ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never
                    )
                )
            )
            .build()

        val result = factory().createForExpressCheckoutElement(
            configuration = configuration,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result?.link?.display)
            .isEqualTo(PaymentSheet.LinkConfiguration.Display.Never)
    }

    @Test
    fun `createForExpressCheckoutElement retains shared payment element options`() {
        val configuration = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration()
                    .preferredNetworks(listOf(CardBrand.CartesBancaires))
                    .paymentMethodOrder(listOf("klarna", "card"))
                    .opensCardScannerAutomatically(true)
                    .termsDisplay(mapOf(PaymentMethod.Type.Card to TermsDisplay.NEVER))
                    .appearance(Appearance().shapes(Appearance.Shapes().cornerRadiusDp(12f)))
            )
            .expressCheckoutElement(ExpressCheckoutElement.Configuration())
            .build()

        val result = requireNotNull(
            factory().createForExpressCheckoutElement(
                configuration = configuration,
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
                collectedDetails = collectedDetails(),
            )
        )

        assertThat(result.preferredNetworks).containsExactly(CardBrand.CartesBancaires)
        assertThat(result.paymentMethodOrder).containsExactly("klarna", "card").inOrder()
        assertThat(result.opensCardScannerAutomatically).isTrue()
        assertThat(result.termsDisplay).containsExactly(PaymentMethod.Type.Card, PaymentSheet.TermsDisplay.NEVER)
        assertThat(result.appearance.shapes.cornerRadiusDp).isEqualTo(12f)
    }

    @Test
    fun `createForExpressCheckoutElement uses defaults when payment element is not configured`() {
        val configuration = CheckoutController.Configuration()
            .expressCheckoutElement(
                ExpressCheckoutElement.Configuration()
                    .googlePayConfiguration(
                        GooglePayConfiguration().label("ECE total").buttonType(
                            GooglePayConfiguration.ButtonType.Donate
                        )
                    )
                    .linkConfiguration(
                        ExpressCheckoutElement.Configuration.LinkConfiguration().display(
                            ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never
                        )
                    )
            )
            .build()

        val result = requireNotNull(
            factory().createForExpressCheckoutElement(
                configuration = configuration,
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = "GB"),
                collectedDetails = collectedDetails(),
            )
        )

        assertThat(result.preferredNetworks).isEqualTo(ConfigurationDefaults.preferredNetworks)
        assertThat(result.paymentMethodOrder).isEqualTo(ConfigurationDefaults.paymentMethodOrder)
        assertThat(result.cardBrandAcceptance).isEqualTo(ConfigurationDefaults.cardBrandAcceptance)
        assertThat(result.termsDisplay).isEmpty()
        assertThat(result.opensCardScannerAutomatically)
            .isEqualTo(ConfigurationDefaults.opensCardScannerAutomatically)
        assertThat(result.appearance).isEqualTo(ConfigurationDefaults.appearance)
        assertThat(result.googlePay?.countryCode).isEqualTo("GB")
        assertThat(result.googlePay?.label).isEqualTo("ECE total")
        assertThat(result.googlePay?.buttonType).isEqualTo(PaymentSheet.GooglePayConfiguration.ButtonType.Donate)
        assertThat(result.link.display).isEqualTo(PaymentSheet.LinkConfiguration.Display.Never)
    }

    @Test
    fun `createForExpressCheckoutElement returns null when express checkout configuration is absent`() {
        val result = factory().createForExpressCheckoutElement(
            configuration = CheckoutController.Configuration().build(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `createForExpressCheckoutElement uses automatic billing collection when session has email`() {
        val configuration = CheckoutController.Configuration()
            .expressCheckoutElement(ExpressCheckoutElement.Configuration())
            .build()

        val result = factory().createForExpressCheckoutElement(
            configuration = configuration,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                customerEmail = "customer@example.com",
            ),
            collectedDetails = collectedDetails(),
        )

        assertThat(result?.billingDetailsCollectionConfiguration?.name)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic)
        assertThat(result?.billingDetailsCollectionConfiguration?.phone)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic)
        assertThat(result?.billingDetailsCollectionConfiguration?.email)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic)
        assertThat(result?.billingDetailsCollectionConfiguration?.address)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic)
        assertThat(result?.billingDetailsCollectionConfiguration?.attachDefaultsToPaymentMethod).isTrue()
    }

    @Test
    fun `createForExpressCheckoutElement collects full address when required by checkout session`() {
        val configuration = CheckoutController.Configuration()
            .expressCheckoutElement(ExpressCheckoutElement.Configuration())
            .build()

        val result = factory().createForExpressCheckoutElement(
            configuration = configuration,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(requiresBillingAddress = true),
            collectedDetails = collectedDetails(),
        )

        assertThat(result?.billingDetailsCollectionConfiguration?.address).isEqualTo(PSFull)
    }

    @Test
    fun `createForExpressCheckoutElement always collects email when no email is provided`() {
        val configuration = CheckoutController.Configuration()
            .expressCheckoutElement(ExpressCheckoutElement.Configuration())
            .build()

        val result = factory().createForExpressCheckoutElement(
            configuration = configuration,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            collectedDetails = collectedDetails(),
        )

        assertThat(result?.billingDetailsCollectionConfiguration?.email)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always)
    }

    @Test
    fun `createForExpressCheckoutElement uses automatic email collection when default email is provided`() {
        val configuration = CheckoutController.Configuration()
            .defaults(
                CheckoutController.Configuration.Defaults().email("default@example.com")
            )
            .expressCheckoutElement(ExpressCheckoutElement.Configuration())
            .build()

        val result = factory().createForExpressCheckoutElement(
            configuration = configuration,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(customerEmail = null),
            collectedDetails = collectedDetails(email = "default@example.com"),
        )

        assertThat(result?.billingDetailsCollectionConfiguration?.email)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic)
    }

    private fun factory() = CheckoutCommonConfigurationFactory(appName = "Test App")

    private fun collectedDetails(email: String? = null) = CheckoutCollectedDetails(
        email = email,
        shippingName = null,
        shippingAddress = null,
    )
}
