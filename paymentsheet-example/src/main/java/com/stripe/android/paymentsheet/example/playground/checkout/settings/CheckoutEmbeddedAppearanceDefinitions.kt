package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.uicore.EmbeddedCheckmarkColors
import com.stripe.android.uicore.EmbeddedDisclosureColors
import com.stripe.android.uicore.EmbeddedRadioColors
import com.stripe.android.uicore.StripeThemeDefaults

internal class CheckoutEmbeddedAppearanceDefinitions(key: String) {
    val rowStyle = RowStyleDefinitions(key = "$key.row_style")
    val paymentMethodIconMargins = CheckoutOptionalInsetsDefinitions(
        key = "$key.payment_method_icon_margins",
        displayName = "paymentMethodIconMargins",
    )
    val titleFont = CheckoutFontDefinitions(
        key = "$key.title_font",
        displayName = "titleFont",
    )
    val subtitleFont = CheckoutFontDefinitions(
        key = "$key.subtitle_font",
        displayName = "subtitleFont",
    )

    val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
        key = key,
        displayName = "embeddedAppearance",
        children = arrayOf(
            rowStyle.configuration,
            paymentMethodIconMargins.configuration,
            titleFont.configuration,
            subtitleFont.configuration,
        ),
    )

    internal class RowStyleDefinitions(key: String) {
        val type = enumChoice(
            key = "$key.type",
            displayName = "rowStyle",
            defaultValue = CheckoutEmbeddedRowStyle.FlatWithRadio,
        )
        val flatWithRadio = FlatWithRadioDefinitions(key = "$key.flat_with_radio")
        val flatWithCheckmark = FlatWithCheckmarkDefinitions(key = "$key.flat_with_checkmark")
        val floatingButton = FloatingButtonDefinitions(key = "$key.floating_button")
        val flatWithDisclosure = FlatWithDisclosureDefinitions(key = "$key.flat_with_disclosure")
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = key,
            displayName = "rowStyle",
            children = arrayOf(
                type,
                flatWithRadio.configuration,
                flatWithCheckmark.configuration,
                floatingButton.configuration,
                flatWithDisclosure.configuration,
            ),
        )
    }

    internal class FlatWithRadioDefinitions(key: String) {
        val flat = CheckoutFlatRowStyleDefinitions(key = key)
        val colorsLight = RadioColorsDefinitions(
            key = "$key.colors_light",
            displayName = "colorsLight",
            defaultColors = StripeThemeDefaults.radioColorsLight,
        )
        val colorsDark = RadioColorsDefinitions(
            key = "$key.colors_dark",
            displayName = "colorsDark",
            defaultColors = StripeThemeDefaults.radioColorsDark,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = key,
            displayName = "FlatWithRadio",
            children = flat.separators + flat.insets + arrayOf(colorsLight.configuration, colorsDark.configuration),
        )
    }

    internal class RadioColorsDefinitions(
        key: String,
        displayName: String,
        defaultColors: EmbeddedRadioColors,
    ) {
        val separatorColor = optionalColor(
            key = "$key.separator_color",
            displayName = "separatorColor",
            defaultValue = defaultColors.separatorColor,
        )
        val selectedColor = optionalColor(
            key = "$key.selected_color",
            displayName = "selectedColor",
            defaultValue = defaultColors.selectedColor,
        )
        val unselectedColor = optionalColor(
            key = "$key.unselected_color",
            displayName = "unselectedColor",
            defaultValue = defaultColors.unselectedColor,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = key,
            displayName = displayName,
            children = arrayOf(separatorColor, selectedColor, unselectedColor),
        )
    }

    internal class FlatWithCheckmarkDefinitions(key: String) {
        val flat = CheckoutFlatRowStyleDefinitions(key = key)
        val checkmarkInsetDp = decimal(
            key = "$key.checkmark_inset_dp",
            displayName = "checkmarkInsetDp",
            defaultValue = StripeThemeDefaults.embeddedCommon.checkmarkInsetDp,
        )
        val colorsLight = CheckmarkColorsDefinitions(
            key = "$key.colors_light",
            displayName = "colorsLight",
            defaultColors = StripeThemeDefaults.checkmarkColorsLight,
        )
        val colorsDark = CheckmarkColorsDefinitions(
            key = "$key.colors_dark",
            displayName = "colorsDark",
            defaultColors = StripeThemeDefaults.checkmarkColorsDark,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = key,
            displayName = "FlatWithCheckmark",
            children = flat.separators + checkmarkInsetDp + flat.insets +
                arrayOf(colorsLight.configuration, colorsDark.configuration),
        )
    }

    internal class CheckmarkColorsDefinitions(
        key: String,
        displayName: String,
        defaultColors: EmbeddedCheckmarkColors,
    ) {
        val separatorColor = optionalColor(
            key = "$key.separator_color",
            displayName = "separatorColor",
            defaultValue = defaultColors.separatorColor,
        )
        val checkmarkColor = optionalColor(
            key = "$key.checkmark_color",
            displayName = "checkmarkColor",
            defaultValue = defaultColors.checkmarkColor,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = key,
            displayName = displayName,
            children = arrayOf(separatorColor, checkmarkColor),
        )
    }

    internal class FloatingButtonDefinitions(key: String) {
        val spacingDp = decimal(
            key = "$key.spacing_dp",
            displayName = "spacingDp",
            defaultValue = StripeThemeDefaults.floating.spacing,
        )
        val additionalInsetsDp = decimal(
            key = "$key.additional_insets_dp",
            displayName = "additionalInsetsDp",
            defaultValue = StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = key,
            displayName = "FloatingButton",
            children = arrayOf(spacingDp, additionalInsetsDp),
        )
    }

    internal class FlatWithDisclosureDefinitions(key: String) {
        val flat = CheckoutFlatRowStyleDefinitions(key = key)
        val colorsLight = DisclosureColorsDefinitions(
            key = "$key.colors_light",
            displayName = "colorsLight",
            defaultColors = StripeThemeDefaults.disclosureColorsLight,
        )
        val colorsDark = DisclosureColorsDefinitions(
            key = "$key.colors_dark",
            displayName = "colorsDark",
            defaultColors = StripeThemeDefaults.disclosureColorsDark,
        )
        val disclosureIconRes = choice(
            key = "$key.disclosure_icon_res",
            displayName = "disclosureIconRes",
            defaultValue = CheckoutDisclosureIcon.Default,
            options = CheckoutDisclosureIcon.entries.map { it.displayName to it },
            serialize = CheckoutDisclosureIcon::serializedValue,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = key,
            displayName = "FlatWithDisclosure",
            children = flat.separators + flat.insets + arrayOf(
                colorsLight.configuration,
                colorsDark.configuration,
                disclosureIconRes,
            ),
        )
    }

    internal class DisclosureColorsDefinitions(
        key: String,
        displayName: String,
        defaultColors: EmbeddedDisclosureColors,
    ) {
        val separatorColor = optionalColor(
            key = "$key.separator_color",
            displayName = "separatorColor",
            defaultValue = defaultColors.separatorColor,
        )
        val disclosureColor = optionalColor(
            key = "$key.disclosure_color",
            displayName = "disclosureColor",
            defaultValue = defaultColors.disclosureColor,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = key,
            displayName = displayName,
            children = arrayOf(separatorColor, disclosureColor),
        )
    }
}

/**
 * Settings shared by the flat row styles. Exposed as two groups so each row style can keep the API's member order.
 */
internal class CheckoutFlatRowStyleDefinitions(key: String) {
    val separatorThicknessDp = decimal(
        key = "$key.separator_thickness_dp",
        displayName = "separatorThicknessDp",
        defaultValue = StripeThemeDefaults.flat.separatorThickness,
    )
    val startSeparatorInsetDp = decimal(
        key = "$key.start_separator_inset_dp",
        displayName = "startSeparatorInsetDp",
        defaultValue = StripeThemeDefaults.flat.separatorInsets,
    )
    val endSeparatorInsetDp = decimal(
        key = "$key.end_separator_inset_dp",
        displayName = "endSeparatorInsetDp",
        defaultValue = StripeThemeDefaults.flat.separatorInsets,
    )
    val topSeparatorEnabled = boolean(
        key = "$key.top_separator_enabled",
        displayName = "topSeparatorEnabled",
        defaultValue = StripeThemeDefaults.flat.topSeparatorEnabled,
    )
    val bottomSeparatorEnabled = boolean(
        key = "$key.bottom_separator_enabled",
        displayName = "bottomSeparatorEnabled",
        defaultValue = StripeThemeDefaults.flat.bottomSeparatorEnabled,
    )
    val additionalVerticalInsetsDp = decimal(
        key = "$key.additional_vertical_insets_dp",
        displayName = "additionalVerticalInsetsDp",
        defaultValue = StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp,
    )
    val horizontalInsetsDp = decimal(
        key = "$key.horizontal_insets_dp",
        displayName = "horizontalInsetsDp",
        defaultValue = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp,
    )
    val separators: Array<CheckoutPlaygroundSettingDefinition> = arrayOf(
        separatorThicknessDp,
        startSeparatorInsetDp,
        endSeparatorInsetDp,
        topSeparatorEnabled,
        bottomSeparatorEnabled,
    )
    val insets: Array<CheckoutPlaygroundSettingDefinition> = arrayOf(
        additionalVerticalInsetsDp,
        horizontalInsetsDp,
    )
}

internal enum class CheckoutEmbeddedRowStyle {
    FlatWithRadio,
    FlatWithCheckmark,
    FloatingButton,
    FlatWithDisclosure,
}
