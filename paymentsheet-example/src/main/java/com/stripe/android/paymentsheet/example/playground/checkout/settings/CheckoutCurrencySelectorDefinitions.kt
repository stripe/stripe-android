@file:OptIn(com.stripe.android.checkout.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.elements.CurrencySelectorElement

internal object CheckoutCurrencySelectorDefinitions {
    val shouldSetConfiguration = boolean(
        key = "controller.currency_selector.should_set_configuration",
        displayName = "Set configuration",
    )

    val appearance = AppearanceDefinitions()

    internal class AppearanceDefinitions {
        val contentVerticalPaddingDp = decimal(
            key = "currency.appearance.content_vertical_padding_dp",
            displayName = "contentVerticalPaddingDp",
            defaultValue = 4f,
        )
        val cornerRadiusDp = optionalFloat(
            key = "currency.appearance.corner_radius_dp",
            displayName = "cornerRadiusDp",
        )
        val borderWidthDp = optionalFloat(
            key = "currency.appearance.border_width_dp",
            displayName = "borderWidthDp",
        )
        val borderColor = optionalColor(
            key = "currency.appearance.border_color",
            displayName = "borderColor",
        )
        val background = optionalColor(
            key = "currency.appearance.background",
            displayName = "background",
        )
        val selectedBackground = optionalColor(
            key = "currency.appearance.selected_background",
            displayName = "selectedBackground",
        )
        val textColor = optionalColor(
            key = "currency.appearance.text_color",
            displayName = "textColor",
        )
        val selectedTextColor = optionalColor(
            key = "currency.appearance.selected_text_color",
            displayName = "selectedTextColor",
        )
        val textSecondaryColor = optionalColor(
            key = "currency.appearance.text_secondary_color",
            displayName = "textSecondaryColor",
        )
        val dangerColor = optionalColor(
            key = "currency.appearance.danger_color",
            displayName = "dangerColor",
        )
        val fontResId = font(key = "currency.appearance.font_res_id", displayName = "fontResId")
        val sizeScaleFactor = decimal(
            key = "currency.appearance.size_scale_factor",
            displayName = "sizeScaleFactor",
            defaultValue = 1f,
            minimumExclusive = true,
        )
        val labelContent = enumChoice(
            key = "currency.appearance.label_content",
            displayName = "labelContent",
            defaultValue = CurrencySelectorElement.Configuration.Appearance.LabelContent.AUTOMATIC,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "currency.appearance",
            displayName = "appearance",
            children = arrayOf(
                contentVerticalPaddingDp,
                cornerRadiusDp,
                borderWidthDp,
                borderColor,
                background,
                selectedBackground,
                textColor,
                selectedTextColor,
                textSecondaryColor,
                dangerColor,
                fontResId,
                sizeScaleFactor,
                labelContent,
            ),
        )
    }

    val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
        key = "controller.currency_selector",
        displayName = "Currency Selector Element",
        children = arrayOf(shouldSetConfiguration, appearance.configuration),
    )
}
