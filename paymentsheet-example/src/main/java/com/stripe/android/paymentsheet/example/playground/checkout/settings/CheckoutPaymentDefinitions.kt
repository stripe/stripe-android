@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

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

    val billing = BillingDefinitions()

    internal class BillingDefinitions {
        val name = enumChoice(
            key = "payment.billing.name",
            displayName = "Name",
            defaultValue = PaymentElement.Configuration.BillingDetailsCollectionConfiguration
                .CollectionMode.Automatic,
        )
        val address = enumChoice(
            key = "payment.billing.address",
            displayName = "Address",
            defaultValue = PaymentElement.Configuration.BillingDetailsCollectionConfiguration
                .AddressCollectionMode.Automatic,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "payment.billing",
            displayName = "Billing details collection",
            children = arrayOf(name, address),
        )
    }

    val layout = enumChoice(
        key = "payment.layout",
        displayName = "Payment method layout",
        defaultValue = PaymentElement.Configuration.PaymentMethodLayout.Automatic,
    )
    val opensCardScanner = boolean(
        key = "payment.opens_card_scanner",
        displayName = "Open card scanner automatically",
        defaultValue = false,
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
        val mode = choice(
            key = "payment.card_brand_acceptance.mode",
            displayName = "Mode",
            defaultValue = CheckoutCardBrandAcceptanceMode.All,
            options = CheckoutCardBrandAcceptanceMode.entries.map { it.name to it },
            serialize = CheckoutCardBrandAcceptanceMode::name,
        )
        val visa = boolean(
            key = "payment.card_brand_acceptance.visa",
            displayName = "Visa",
            defaultValue = false,
        )
        val mastercard = boolean(
            key = "payment.card_brand_acceptance.mastercard",
            displayName = "Mastercard",
            defaultValue = false,
        )
        val amex = boolean(
            key = "payment.card_brand_acceptance.amex",
            displayName = "Amex",
            defaultValue = false,
        )
        val discover = boolean(
            key = "payment.card_brand_acceptance.discover",
            displayName = "Discover",
            defaultValue = false,
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
        val lightColors = CheckoutPaymentColorsDefinitions(
            key = "payment.appearance.colors_light",
            displayName = "Light colors",
            defaultColors = StripeThemeDefaults.colorsLight,
        )
        val darkColors = CheckoutPaymentColorsDefinitions(
            key = "payment.appearance.colors_dark",
            displayName = "Dark colors",
            defaultColors = StripeThemeDefaults.colorsDark,
        )
        val themeMode = enumChoice(
            key = "payment.appearance.theme_mode",
            displayName = "Theme mode",
            defaultValue = PaymentElement.Configuration.Appearance.ThemeMode.Automatic,
        )

        val primaryButton = PrimaryButtonDefinitions()

        internal class PrimaryButtonDefinitions {
            val lightColors = CheckoutPrimaryButtonColorsDefinitions(
                key = "payment.appearance.primary_button.colors_light",
                displayName = "Light colors",
            )
            val darkColors = CheckoutPrimaryButtonColorsDefinitions(
                key = "payment.appearance.primary_button.colors_dark",
                displayName = "Dark colors",
            )

            val shape = ShapeDefinitions()

            internal class ShapeDefinitions {
                val cornerRadius = optionalFloat(
                    key = "payment.appearance.primary_button.shape.corner",
                    displayName = "Corner radius",
                    minimum = 0f,
                )
                val borderWidth = optionalFloat(
                    key = "payment.appearance.primary_button.shape.border",
                    displayName = "Border width",
                    minimum = 0f,
                )
                val height = optionalFloat(
                    key = "payment.appearance.primary_button.shape.height",
                    displayName = "Height",
                    minimum = 0f,
                )
                val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
                    key = "payment.appearance.primary_button.shape",
                    displayName = "Shape",
                    children = arrayOf(cornerRadius, borderWidth, height),
                )
            }

            val typography = TypographyDefinitions()

            internal class TypographyDefinitions {
                val font = font(key = "payment.appearance.primary_button.typography.font")
                val size = optionalFloat(
                    key = "payment.appearance.primary_button.typography.size",
                    displayName = "Font size",
                    minimum = 0f,
                    minimumExclusive = true,
                )
                val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
                    key = "payment.appearance.primary_button.typography",
                    displayName = "Typography",
                    children = arrayOf(font, size),
                )
            }

            val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
                key = "payment.appearance.primary_button",
                displayName = "Primary button",
                children = arrayOf(
                    lightColors.configuration,
                    darkColors.configuration,
                    shape.configuration,
                    typography.configuration,
                ),
            )
        }

        val insets = InsetsDefinitions()

        internal class InsetsDefinitions {
            val horizontal = decimal(
                key = "payment.appearance.insets.horizontal",
                displayName = "Horizontal",
                defaultValue = 20f,
                minimum = 0f,
            )
            val vertical = decimal(
                key = "payment.appearance.insets.vertical",
                displayName = "Vertical",
                defaultValue = 0f,
                minimum = 0f,
            )
            val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
                key = "payment.appearance.insets",
                displayName = "Form insets",
                children = arrayOf(horizontal, vertical),
            )
        }

        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "payment.appearance",
            displayName = "Appearance",
            children = arrayOf(
                lightColors.configuration,
                darkColors.configuration,
                themeMode,
                primaryButton.configuration,
                insets.configuration,
            ),
        )
    }

    val googlePay = CheckoutGooglePayDefinitions(
        key = "payment.google_pay",
        displayName = "Google Pay",
        defaultDisplay = PaymentElement.Configuration.GooglePayConfiguration.Display.Automatic,
        displayOptions = PaymentElement.Configuration.GooglePayConfiguration.Display.entries,
        defaultButtonType = PaymentElement.Configuration.GooglePayConfiguration.ButtonType.Pay,
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
                billing.configuration,
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
