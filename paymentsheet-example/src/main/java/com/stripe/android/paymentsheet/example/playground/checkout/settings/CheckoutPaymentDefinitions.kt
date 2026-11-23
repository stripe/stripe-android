@file:OptIn(com.stripe.android.checkout.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.elements.PaymentElement
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.uicore.StripeThemeDefaults

internal object CheckoutPaymentDefinitions {
    val shouldSetConfiguration = boolean(
        key = "controller.payment_element.should_set_configuration",
        displayName = "Set configuration",
        defaultValue = true,
    )
    val embeddedMandate = boolean(
        key = "payment.embedded_mandate",
        displayName = "Embedded view displays mandate",
        defaultValue = true,
    )

    val layout = enumChoice(
        key = "payment.layout",
        displayName = "Payment method layout",
        defaultValue = PaymentElement.Configuration.PaymentMethodLayout.Automatic,
    )
    val opensCardScanner = boolean(
        key = "payment.opens_card_scanner",
        displayName = "Open card scanner automatically",
    )
    val preferredNetworks = csv(
        key = "payment.preferred_networks",
        displayName = "Preferred networks (comma separated)",
        decodeItem = { code ->
            CardBrand.entries.firstOrNull { it.code == code }
                ?.let(Result.Companion::success)
                ?: invalid(message = "Unknown value: $code")
        },
        encodeItem = CardBrand::code,
    )
    val methodOrder = stringCsv(
        key = "payment.method_order",
        displayName = "Payment method order (comma separated)",
    )

    val cardBrandAcceptance = CardBrandAcceptanceDefinitions()

    internal class CardBrandAcceptanceDefinitions {
        val mode = enumChoice(
            key = "payment.card_brand_acceptance.mode",
            displayName = "Mode",
            defaultValue = CheckoutCardBrandAcceptanceMode.All,
        )
        val visa = boolean(
            key = "payment.card_brand_acceptance.visa",
            displayName = "Visa",
        )
        val mastercard = boolean(
            key = "payment.card_brand_acceptance.mastercard",
            displayName = "Mastercard",
        )
        val amex = boolean(
            key = "payment.card_brand_acceptance.amex",
            displayName = "Amex",
        )
        val discover = boolean(
            key = "payment.card_brand_acceptance.discover",
            displayName = "Discover",
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "payment.card_brand_acceptance",
            displayName = "Card brand acceptance",
            children = arrayOf(
                mode,
                visa,
                mastercard,
                amex,
                discover,
            ),
        )
    }

    val terms = TermsDefinitions()

    internal class TermsDefinitions {
        val values = PaymentMethod.Type.entries.associateWith { type ->
            choice(
                key = "payment.terms.${type.code}",
                displayName = type.code,
                defaultValue = CheckoutTermsDisplay.Automatic,
                options = CheckoutTermsDisplay.entries.map { it.displayName to it },
                serialize = CheckoutTermsDisplay::serializedValue,
            )
        }
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "payment.terms",
            displayName = "Terms display",
            children = values.values.toTypedArray(),
        )
    }

    val appearance = AppearanceDefinitions()

    internal class AppearanceDefinitions {
        val colorsLight = CheckoutColorsDefinitions(
            key = "payment.appearance.colors_light",
            displayName = "colorsLight",
            defaultColors = StripeThemeDefaults.colorsLight,
        )
        val colorsDark = CheckoutColorsDefinitions(
            key = "payment.appearance.colors_dark",
            displayName = "colorsDark",
            defaultColors = StripeThemeDefaults.colorsDark,
        )
        val themeMode = enumChoice(
            key = "payment.appearance.theme_mode",
            displayName = "themeMode",
            defaultValue = PaymentElement.Configuration.Appearance.ThemeMode.Automatic,
        )
        val primaryButton = CheckoutPrimaryButtonDefinitions(
            key = "payment.appearance.primary_button",
            includeSuccessColors = true,
        )
        val typography = CheckoutTypographyDefinitions(key = "payment.appearance.typography")
        val shapes = CheckoutShapesDefinitions(key = "payment.appearance.shapes")
        val embeddedAppearance = CheckoutEmbeddedAppearanceDefinitions(key = "payment.appearance.embedded_appearance")
        val formInsetValues = CheckoutInsetsDefinitions(
            key = "payment.appearance.form_inset_values",
            displayName = "formInsetValues",
            defaultStartDp = StripeThemeDefaults.formInsets.start,
            defaultTopDp = StripeThemeDefaults.formInsets.top,
            defaultEndDp = StripeThemeDefaults.formInsets.end,
            defaultBottomDp = StripeThemeDefaults.formInsets.bottom,
        )

        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "payment.appearance",
            displayName = "appearance",
            children = arrayOf(
                colorsLight.configuration,
                colorsDark.configuration,
                themeMode,
                primaryButton.configuration,
                typography.configuration,
                shapes.configuration,
                embeddedAppearance.configuration,
                formInsetValues.configuration,
            ),
        )
    }

    val googlePay = CheckoutGooglePayDefinitions(
        key = "payment.google_pay",
        displayName = "Google Pay",
        displayOptions = PaymentElement.Configuration.GooglePayConfiguration.Display.entries,
        buttonTypeOptions = PaymentElement.Configuration.GooglePayConfiguration.ButtonType.entries,
    )

    val link = LinkDefinitions()

    internal class LinkDefinitions {
        val display = enumChoice(
            key = "payment.link.display",
            displayName = "Display",
            defaultValue = PaymentElement.Configuration.LinkConfiguration.Display.Automatic,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "payment.link",
            displayName = "Link",
            children = arrayOf(display),
        )
    }

    val configuration: CheckoutPlaygroundSettingDefinition.Configuration by lazy {
        configuration(
            key = "controller.payment_element",
            displayName = "Payment Element",
            children = arrayOf(
                shouldSetConfiguration,
                embeddedMandate,
                layout,
                opensCardScanner,
                preferredNetworks,
                methodOrder,
                cardBrandAcceptance.configuration,
                terms.configuration,
                appearance.configuration,
                googlePay.configuration,
                link.configuration,
            ),
        )
    }
}
