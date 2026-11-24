@file:OptIn(com.stripe.android.checkout.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.uicore.StripeThemeDefaults

internal object CheckoutShippingAddressDefinitions {
    val shouldSetConfiguration = boolean(
        key = "controller.shipping_address.should_set_configuration",
        displayName = "Set configuration",
    )

    val title = optionalText(
        key = "shipping_address.title",
        displayName = "Title",
    )
    val buttonTitle = optionalText(
        key = "shipping_address.button_title",
        displayName = "Button title",
    )

    val appearance = AppearanceDefinitions()

    internal class AppearanceDefinitions {
        val colorsLight = CheckoutColorsDefinitions(
            key = "shipping_address.appearance.colors_light",
            displayName = "colorsLight",
            defaultColors = StripeThemeDefaults.colorsLight,
        )
        val colorsDark = CheckoutColorsDefinitions(
            key = "shipping_address.appearance.colors_dark",
            displayName = "colorsDark",
            defaultColors = StripeThemeDefaults.colorsDark,
        )
        val themeMode = enumChoice(
            key = "shipping_address.appearance.theme_mode",
            displayName = "themeMode",
            defaultValue = ShippingAddressElement.Configuration.Appearance.ThemeMode.Automatic,
        )
        val primaryButton = CheckoutPrimaryButtonDefinitions(
            key = "shipping_address.appearance.primary_button",
            includeSuccessColors = false,
        )
        val formInsetValues = CheckoutInsetsDefinitions(
            key = "shipping_address.appearance.form_inset_values",
            displayName = "formInsetValues",
            defaultStartDp = StripeThemeDefaults.formInsets.start,
            defaultTopDp = StripeThemeDefaults.formInsets.top,
            defaultEndDp = StripeThemeDefaults.formInsets.end,
            defaultBottomDp = StripeThemeDefaults.formInsets.bottom,
        )
        val shapes = CheckoutShapesDefinitions(key = "shipping_address.appearance.shapes")
        val typography = CheckoutTypographyDefinitions(key = "shipping_address.appearance.typography")

        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "shipping_address.appearance",
            displayName = "appearance",
            children = arrayOf(
                colorsLight.configuration,
                colorsDark.configuration,
                themeMode,
                primaryButton.configuration,
                formInsetValues.configuration,
                shapes.configuration,
                typography.configuration,
            ),
        )
    }

    val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
        key = "controller.shipping_address",
        displayName = "Shipping Address Element",
        children = arrayOf(shouldSetConfiguration, title, buttonTitle, appearance.configuration),
    )
}
