package com.stripe.android.checkout.analytics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.elements.CurrencySelectorElement
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.PaymentElement
import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class, AppearanceAPIAdditionsPreview::class)
class CheckoutConfigurationAnalyticsTest {
    @Test
    fun `default configuration reports presence and absent optional elements`() {
        val params = CheckoutController.Configuration()
            .build()
            .toAnalyticsMap()

        assertThat(params["merchant_display_name"]).isEqualTo(false)
        assertThat(params["defaults"]).isEqualTo(
            mapOf(
                "billing_details" to mapOf("name" to false, "address" to false),
                "shipping_details" to mapOf("name" to false, "address" to false),
                "email" to false,
                "phone" to false,
            )
        )
        assertThat(params["payment_element"]).isNull()
        assertThat(params["express_checkout_element"]).isNull()
        assertThat(params["currency_selector_element"]).isNull()
        assertThat(params["shipping_address_element"]).isNull()
    }

    @Test
    fun `default payment element records default choices`() {
        val params = CheckoutController.Configuration()
            .paymentElement(PaymentElement.Configuration())
            .build()
            .toAnalyticsMap()
        val paymentElement = params["payment_element"] as Map<*, *>

        assertThat(paymentElement["payment_method_layout"]).isEqualTo("automatic")
        assertThat(paymentElement["preferred_networks"]).isEqualTo(emptyList<String>())
        assertThat(paymentElement["payment_method_order"]).isEqualTo(emptyList<String>())
        assertThat(paymentElement["terms_display"]).isEqualTo(emptyMap<String, String>())
        assertThat(paymentElement["card_brand_acceptance"]).isEqualTo(
            mapOf("type" to "all", "brands" to emptyList<String>())
        )
        assertAppearanceColorFlags(paymentElement["appearance"] as Map<*, *>, customized = false)
    }

    @Test
    fun `payment element serializes customized appearance`() {
        val appearance = PaymentElement.Configuration.Appearance()
            .colorsLight(PaymentElement.Configuration.Appearance.Colors.light().primary(Color.Red))
            .primaryButton(
                PaymentElement.Configuration.Appearance.PrimaryButton().colorsDark(
                    PaymentElement.Configuration.Appearance.PrimaryButton.Colors.dark().background(Color.Blue)
                )
            )
        val params = CheckoutController.Configuration()
            .paymentElement(PaymentElement.Configuration().appearance(appearance))
            .build()
            .toAnalyticsMap()
        val paymentElement = params["payment_element"] as Map<*, *>

        assertAppearanceColorFlags(paymentElement["appearance"] as Map<*, *>, customized = true)
    }

    @Test
    fun `shipping address serializes customized appearance`() {
        val appearance = ShippingAddressElement.Configuration.Appearance()
            .colorsLight(ShippingAddressElement.Configuration.Appearance.Colors.light().primary(Color.Red))
            .primaryButton(
                ShippingAddressElement.Configuration.Appearance.PrimaryButton().colorsDark(
                    ShippingAddressElement.Configuration.Appearance.PrimaryButton.Colors.dark().background(Color.Blue)
                )
            )
        val params = CheckoutController.Configuration()
            .shippingAddressElement(ShippingAddressElement.Configuration().appearance(appearance))
            .build()
            .toAnalyticsMap()
        val shippingAddressElement = params["shipping_address_element"] as Map<*, *>

        assertAppearanceColorFlags(shippingAddressElement["appearance"] as Map<*, *>, customized = true)
    }

    @Test
    fun `payment element serializes safe values and preserves list order`() {
        val configuration = PaymentElement.Configuration()
            .embeddedViewDisplaysMandateText(false)
            .paymentMethodLayout(PaymentElement.Configuration.PaymentMethodLayout.Vertical)
            .opensCardScannerAutomatically(true)
            .preferredNetworks(listOf(CardBrand.AmericanExpress, CardBrand.Visa))
            .paymentMethodOrder(listOf("klarna", "card"))
            .cardBrandAcceptance(
                PaymentElement.Configuration.CardBrandAcceptance.allowed(
                    listOf(PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Amex)
                )
            )
            .termsDisplay(mapOf(PaymentMethod.Type.Card to PaymentElement.Configuration.TermsDisplay.NEVER))
            .googlePayConfiguration(
                PaymentElement.Configuration.GooglePayConfiguration()
                    .display(PaymentElement.Configuration.GooglePayConfiguration.Display.Never)
                    .buttonType(PaymentElement.Configuration.GooglePayConfiguration.ButtonType.Subscribe)
                    .additionalEnabledNetworks(listOf("INTERAC", "JCB"))
            )
            .linkConfiguration(
                PaymentElement.Configuration.LinkConfiguration()
                    .display(PaymentElement.Configuration.LinkConfiguration.Display.WalletButtonHidden)
            )

        val params = CheckoutController.Configuration()
            .paymentElement(configuration)
            .build()
            .toAnalyticsMap()
        val paymentElement = params["payment_element"] as Map<*, *>

        assertThat(paymentElement["embedded_view_displays_mandate_text"]).isEqualTo(false)
        assertThat(paymentElement["payment_method_layout"]).isEqualTo("vertical")
        assertThat(paymentElement["opens_card_scanner_automatically"]).isEqualTo(true)
        assertThat(paymentElement["preferred_networks"])
            .isEqualTo(listOf(CardBrand.AmericanExpress.code, CardBrand.Visa.code))
        assertThat(paymentElement["payment_method_order"]).isEqualTo(listOf("klarna", "card"))
        assertThat(paymentElement["card_brand_acceptance"]).isEqualTo(
            mapOf("type" to "allowed", "brands" to listOf("amex"))
        )
        assertThat(paymentElement["terms_display"]).isEqualTo(mapOf("card" to "never"))
        assertThat(paymentElement["link_configuration"])
            .isEqualTo(mapOf("display" to "wallet_button_hidden"))
        val googlePay = paymentElement["google_pay_configuration"] as Map<*, *>
        assertThat(googlePay["display"]).isEqualTo("never")
        assertThat(googlePay["button_type"]).isEqualTo("subscribe")
        assertThat(googlePay["additional_enabled_networks"]).isEqualTo(listOf("INTERAC", "JCB"))
    }

    @Test
    fun `card brand acceptance records disallowed brands in order`() {
        val params = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration().cardBrandAcceptance(
                    PaymentElement.Configuration.CardBrandAcceptance.disallowed(
                        listOf(
                            PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Visa,
                            PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Discover,
                        )
                    )
                )
            )
            .build()
            .toAnalyticsMap()
        val paymentElement = params["payment_element"] as Map<*, *>

        assertThat(paymentElement["card_brand_acceptance"]).isEqualTo(
            mapOf("type" to "disallowed", "brands" to listOf("visa", "discover"))
        )
    }

    @Test
    fun `express checkout serializes safe values and appearance`() {
        val configuration = ExpressCheckoutElement.Configuration()
            .linkConfiguration(
                ExpressCheckoutElement.Configuration.LinkConfiguration()
                    .display(ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never)
            )
            .googlePayConfiguration(
                ExpressCheckoutElement.Configuration.GooglePayConfiguration()
                    .display(ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never)
                    .buttonType(ExpressCheckoutElement.Configuration.GooglePayConfiguration.ButtonType.Checkout)
                    .additionalEnabledNetworks(listOf("DISCOVER", "INTERAC"))
            )
            .appearance(
                ExpressCheckoutElement.Configuration.Appearance()
                    .buttonLayout(
                        ExpressCheckoutElement.Configuration.Appearance.ButtonLayout()
                            .maxColumns(2)
                            .maxRows(3)
                    )
                    .buttonTheme(ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Dark)
                    .cornerRadiusDp(6f)
            )

        val params = CheckoutController.Configuration()
            .expressCheckoutElement(configuration)
            .build()
            .toAnalyticsMap()
        val expressCheckoutElement = params["express_checkout_element"] as Map<*, *>

        assertThat(expressCheckoutElement["link_configuration"]).isEqualTo(mapOf("display" to "never"))
        val googlePay = expressCheckoutElement["google_pay_configuration"] as Map<*, *>
        assertThat(googlePay["display"]).isEqualTo("never")
        assertThat(googlePay["button_type"]).isEqualTo("checkout")
        assertThat(googlePay["additional_enabled_networks"]).isEqualTo(listOf("DISCOVER", "INTERAC"))
        val appearance = expressCheckoutElement["appearance"] as Map<*, *>
        assertThat(appearance["button_layout"]).isEqualTo(mapOf("max_columns" to 2, "max_rows" to 3))
        assertThat(appearance["button_theme"]).isEqualTo("dark")
        assertThat(appearance["corner_radius_dp"]).isEqualTo(6f)
    }

    @Test
    fun `currency selector records appearance values and shipping reuses appearance serializer`() {
        val currencySelectorConfiguration = CurrencySelectorElement.Configuration()
            .appearance(
                CurrencySelectorElement.Configuration.Appearance()
                    .contentVerticalPaddingDp(6f)
                    .cornerRadiusDp(8f)
                    .borderColor(Color.Red)
                    .background(Color.Blue)
                    .fontResId(1234)
                    .sizeScaleFactor(1.5f)
                    .labelContent(CurrencySelectorElement.Configuration.Appearance.LabelContent.CURRENCY_CODE)
            )
        val shippingAddressConfiguration = ShippingAddressElement.Configuration()
            .appearance(
                ShippingAddressElement.Configuration.Appearance().shapes(
                    ShippingAddressElement.Configuration.Appearance.Shapes().cornerRadiusDp(12f)
                )
            )

        val params = CheckoutController.Configuration()
            .currencySelectorElement(currencySelectorConfiguration)
            .shippingAddressElement(shippingAddressConfiguration)
            .build()
            .toAnalyticsMap()

        val currencySelectorElement = params["currency_selector_element"] as Map<*, *>
        val currencyAppearance = currencySelectorElement["appearance"] as Map<*, *>
        assertThat(currencyAppearance["content_vertical_padding_dp"]).isEqualTo(6f)
        assertThat(currencyAppearance["corner_radius_dp"]).isEqualTo(8f)
        assertThat(currencyAppearance["border_color"]).isEqualTo(Color.Red.toArgb())
        assertThat(currencyAppearance["background"]).isEqualTo(Color.Blue.toArgb())
        assertThat(currencyAppearance["font_res_id"]).isEqualTo(1234)
        assertThat(currencyAppearance["size_scale_factor"]).isEqualTo(1.5f)
        assertThat(currencyAppearance["label_content"]).isEqualTo("currency_code")

        val shippingAddressElement = params["shipping_address_element"] as Map<*, *>
        val shippingAppearance = shippingAddressElement["appearance"] as Map<*, *>
        assertThat(shippingAppearance["corner_radius"]).isEqualTo(true)
        assertAppearanceColorFlags(shippingAppearance, customized = false)
    }

    @Test
    fun `merchant and default customer details serialize only presence`() {
        val privateValues = listOf(
            "private merchant name",
            "private billing name",
            "private billing street",
            "private shipping name",
            "private shipping street",
            "private@example.com",
            "+15555550123",
        )
        val defaults = CheckoutController.Configuration.Defaults()
            .billingDetails(
                CheckoutController.Configuration.Defaults.ContactDetails()
                    .name(privateValues[1])
                    .address(CheckoutController.Address(country = "US").line1(privateValues[2]))
            )
            .shippingDetails(
                CheckoutController.Configuration.Defaults.ContactDetails()
                    .name(privateValues[3])
                    .address(CheckoutController.Address(country = "US").line1(privateValues[4]))
            )
            .email(privateValues[5])
            .phone(privateValues[6])

        val params = CheckoutController.Configuration()
            .merchantDisplayName(privateValues[0])
            .defaults(defaults)
            .build()
            .toAnalyticsMap()

        assertThat(params["merchant_display_name"]).isEqualTo(true)
        assertThat(params["defaults"]).isEqualTo(
            mapOf(
                "billing_details" to mapOf("name" to true, "address" to true),
                "shipping_details" to mapOf("name" to true, "address" to true),
                "email" to true,
                "phone" to true,
            )
        )
        assertNoPrivateValues(params, privateValues)
    }

    @Test
    fun `custom element text serializes only presence`() {
        val privateValues = listOf(
            "private payment label",
            "private express label",
            "private shipping title",
            "private button title",
        )
        val params = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration().googlePayConfiguration(
                    PaymentElement.Configuration.GooglePayConfiguration().label(privateValues[0])
                )
            )
            .expressCheckoutElement(
                ExpressCheckoutElement.Configuration().googlePayConfiguration(
                    ExpressCheckoutElement.Configuration.GooglePayConfiguration().label(privateValues[1])
                )
            )
            .shippingAddressElement(
                ShippingAddressElement.Configuration()
                    .title(privateValues[2])
                    .buttonTitle(privateValues[3])
            )
            .build()
            .toAnalyticsMap()

        val paymentElement = params["payment_element"] as Map<*, *>
        val paymentGooglePay = paymentElement["google_pay_configuration"] as Map<*, *>
        assertThat(paymentGooglePay["label"]).isEqualTo(true)
        val expressCheckoutElement = params["express_checkout_element"] as Map<*, *>
        val expressGooglePay = expressCheckoutElement["google_pay_configuration"] as Map<*, *>
        assertThat(expressGooglePay["label"]).isEqualTo(true)
        val shippingAddressElement = params["shipping_address_element"] as Map<*, *>
        assertThat(shippingAddressElement["title"]).isEqualTo(true)
        assertThat(shippingAddressElement["button_title"]).isEqualTo(true)
        assertNoPrivateValues(params, privateValues)
    }

    private fun assertAppearanceColorFlags(appearance: Map<*, *>, customized: Boolean) {
        assertThat(appearance["colors_light"]).isEqualTo(customized)
        assertThat(appearance["colors_dark"]).isEqualTo(false)
        val primaryButton = appearance["primary_button"] as Map<*, *>
        assertThat(primaryButton["colors_light"]).isEqualTo(false)
        assertThat(primaryButton["colors_dark"]).isEqualTo(customized)
    }

    private fun assertNoPrivateValues(params: Map<String, Any?>, privateValues: List<String>) {
        val serializedParams = params.toString()
        privateValues.forEach { privateValue ->
            assertThat(serializedParams).doesNotContain(privateValue)
        }
    }
}
