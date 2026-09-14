@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.elements.CurrencySelectorElement

internal object CheckoutCurrencySelectorDefinitions {
    val shouldSetConfiguration = boolean(
        key = "controller.currency_selector.should_set_configuration",
        displayName = "Set configuration",
        defaultValue = false,
    )

    val appearance = AppearanceDefinitions()

    internal class AppearanceDefinitions {
        val verticalPadding = decimal(
            key = "currency.appearance.vertical_padding",
            displayName = "Vertical padding",
            defaultValue = 4f,
            minimum = 0f,
        )
        val cornerRadius = optionalFloat(
            key = "currency.appearance.corner_radius",
            displayName = "Corner radius",
            minimum = 0f,
        )
        val borderWidth = optionalFloat(
            key = "currency.appearance.border_width",
            displayName = "Border width",
            minimum = 0f,
        )
        val borderColor = optionalColor(
            key = "currency.appearance.border_color",
            displayName = "Border color",
        )
        val background = optionalColor(
            key = "currency.appearance.background",
            displayName = "Background",
        )
        val selectedBackground = optionalColor(
            key = "currency.appearance.selected_background",
            displayName = "Selected background",
        )
        val textColor = optionalColor(
            key = "currency.appearance.text_color",
            displayName = "Text color",
        )
        val selectedTextColor = optionalColor(
            key = "currency.appearance.selected_text_color",
            displayName = "Selected text color",
        )
        val secondaryTextColor = optionalColor(
            key = "currency.appearance.secondary_text_color",
            displayName = "Secondary text color",
        )
        val dangerColor = optionalColor(
            key = "currency.appearance.danger_color",
            displayName = "Danger color",
        )
        val font = font(key = "currency.appearance.font")
        val scale = decimal(
            key = "currency.appearance.scale",
            displayName = "Size scale factor",
            defaultValue = 1f,
            minimum = 0f,
            minimumExclusive = true,
        )
        val label = enumChoice(
            key = "currency.appearance.label",
            displayName = "Label content",
            defaultValue = CurrencySelectorElement.Configuration.Appearance.LabelContent.AUTOMATIC,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "currency.appearance",
            displayName = "Appearance",
            children = arrayOf(
                verticalPadding,
                cornerRadius,
                borderWidth,
                borderColor,
                background,
                selectedBackground,
                textColor,
                selectedTextColor,
                secondaryTextColor,
                dangerColor,
                font,
                scale,
                label,
            ),
        )
    }

    val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
        key = "controller.currency_selector",
        displayName = "Currency Selector Element",
        children = arrayOf(shouldSetConfiguration, appearance.configuration),
    )
}
