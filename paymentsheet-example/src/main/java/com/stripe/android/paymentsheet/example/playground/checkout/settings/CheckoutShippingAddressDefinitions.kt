@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.uicore.StripeThemeDefaults

internal object CheckoutShippingAddressDefinitions {
    val shouldSetConfiguration = boolean(
        key = "controller.shipping_address.should_set_configuration",
        displayName = "Set configuration",
    )

    val appearance = AppearanceDefinitions()

    internal class AppearanceDefinitions {
        val lightColors = CheckoutPaymentColorsDefinitions(
            key = "shipping_address.appearance.colors_light",
            displayName = "Light colors",
            defaultColors = StripeThemeDefaults.colorsLight,
        )
        val darkColors = CheckoutPaymentColorsDefinitions(
            key = "shipping_address.appearance.colors_dark",
            displayName = "Dark colors",
            defaultColors = StripeThemeDefaults.colorsDark,
        )
        val themeMode = enumChoice(
            key = "shipping_address.appearance.theme_mode",
            displayName = "Theme mode",
            defaultValue = ShippingAddressElement.Configuration.Appearance.ThemeMode.Automatic,
        )

        val primaryButton = PrimaryButtonDefinitions()

        internal class PrimaryButtonDefinitions {
            val lightColors = CheckoutPrimaryButtonColorsDefinitions(
                key = "shipping_address.appearance.primary_button.colors_light",
                displayName = "Light colors",
                includeSuccessColors = false,
            )
            val darkColors = CheckoutPrimaryButtonColorsDefinitions(
                key = "shipping_address.appearance.primary_button.colors_dark",
                displayName = "Dark colors",
                includeSuccessColors = false,
            )

            val shape = ShapeDefinitions()

            internal class ShapeDefinitions {
                val cornerRadius = optionalFloat(
                    key = "shipping_address.appearance.primary_button.shape.corner",
                    displayName = "Corner radius",
                )
                val borderWidth = optionalFloat(
                    key = "shipping_address.appearance.primary_button.shape.border",
                    displayName = "Border width",
                )
                val height = optionalFloat(
                    key = "shipping_address.appearance.primary_button.shape.height",
                    displayName = "Height",
                )
                val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
                    key = "shipping_address.appearance.primary_button.shape",
                    displayName = "Shape",
                    children = arrayOf(cornerRadius, borderWidth, height),
                )
            }

            val typography = TypographyDefinitions()

            internal class TypographyDefinitions {
                val font = font(key = "shipping_address.appearance.primary_button.typography.font")
                val size = optionalFloat(
                    key = "shipping_address.appearance.primary_button.typography.size",
                    displayName = "Font size",
                    minimumExclusive = true,
                )
                val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
                    key = "shipping_address.appearance.primary_button.typography",
                    displayName = "Typography",
                    children = arrayOf(font, size),
                )
            }

            val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
                key = "shipping_address.appearance.primary_button",
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
            val start = decimal(
                key = "shipping_address.appearance.insets.start",
                displayName = "Start",
                defaultValue = 20f,
            )
            val top = decimal(
                key = "shipping_address.appearance.insets.top",
                displayName = "Top",
            )
            val end = decimal(
                key = "shipping_address.appearance.insets.end",
                displayName = "End",
                defaultValue = 20f,
            )
            val bottom = decimal(
                key = "shipping_address.appearance.insets.bottom",
                displayName = "Bottom",
                defaultValue = 40f,
            )
            val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
                key = "shipping_address.appearance.insets",
                displayName = "Form insets",
                children = arrayOf(start, top, end, bottom),
            )
        }

        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "shipping_address.appearance",
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

    val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
        key = "controller.shipping_address",
        displayName = "Shipping Address Element",
        children = arrayOf(shouldSetConfiguration, appearance.configuration),
    )
}
