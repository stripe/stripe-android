package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.uicore.StripeColors
import com.stripe.android.uicore.StripeThemeDefaults

internal class CheckoutColorsDefinitions(
    key: String,
    displayName: String,
    defaultColors: StripeColors,
) {
    val primary = optionalColor(
        key = "$key.primary",
        displayName = "primary",
        defaultValue = defaultColors.materialColors.primary,
    )
    val surface = optionalColor(
        key = "$key.surface",
        displayName = "surface",
        defaultValue = defaultColors.materialColors.surface,
    )
    val component = optionalColor(
        key = "$key.component",
        displayName = "component",
        defaultValue = defaultColors.component,
    )
    val componentBorder = optionalColor(
        key = "$key.component_border",
        displayName = "componentBorder",
        defaultValue = defaultColors.componentBorder,
    )
    val componentDivider = optionalColor(
        key = "$key.component_divider",
        displayName = "componentDivider",
        defaultValue = defaultColors.componentDivider,
    )
    val onComponent = optionalColor(
        key = "$key.on_component",
        displayName = "onComponent",
        defaultValue = defaultColors.onComponent,
    )
    val subtitle = optionalColor(
        key = "$key.subtitle",
        displayName = "subtitle",
        defaultValue = defaultColors.subtitle,
    )
    val placeholderText = optionalColor(
        key = "$key.placeholder_text",
        displayName = "placeholderText",
        defaultValue = defaultColors.placeholderText,
    )
    val onSurface = optionalColor(
        key = "$key.on_surface",
        displayName = "onSurface",
        defaultValue = defaultColors.materialColors.onSurface,
    )
    val appBarIcon = optionalColor(
        key = "$key.app_bar_icon",
        displayName = "appBarIcon",
        defaultValue = defaultColors.appBarIcon,
    )
    val error = optionalColor(
        key = "$key.error",
        displayName = "error",
        defaultValue = defaultColors.materialColors.error,
    )
    val configuration = configuration(
        key = key,
        displayName = displayName,
        children = arrayOf(
            primary,
            surface,
            component,
            componentBorder,
            componentDivider,
            onComponent,
            subtitle,
            placeholderText,
            onSurface,
            appBarIcon,
            error,
        ),
    )
}

internal class CheckoutPrimaryButtonDefinitions(
    key: String,
    includeSuccessColors: Boolean,
) {
    val colorsLight = ColorsDefinitions(
        key = "$key.colors_light",
        displayName = "colorsLight",
        includeSuccessColors = includeSuccessColors,
    )
    val colorsDark = ColorsDefinitions(
        key = "$key.colors_dark",
        displayName = "colorsDark",
        includeSuccessColors = includeSuccessColors,
    )
    val shape = ShapeDefinitions(key = "$key.shape")
    val typography = TypographyDefinitions(key = "$key.typography")
    val configuration = configuration(
        key = key,
        displayName = "primaryButton",
        children = arrayOf(
            colorsLight.configuration,
            colorsDark.configuration,
            shape.configuration,
            typography.configuration,
        ),
    )

    internal class ColorsDefinitions(
        key: String,
        displayName: String,
        includeSuccessColors: Boolean,
    ) {
        val background = optionalColor(key = "$key.background", displayName = "background")
        val onBackground = optionalColor(key = "$key.on_background", displayName = "onBackground")
        val border = optionalColor(key = "$key.border", displayName = "border")
        val successBackgroundColor = optionalColor(
            key = "$key.success_background_color",
            displayName = "successBackgroundColor",
        )
        val onSuccessBackgroundColor = optionalColor(
            key = "$key.on_success_background_color",
            displayName = "onSuccessBackgroundColor",
        )
        val configuration = configuration(
            key = key,
            displayName = displayName,
            children = buildList {
                add(background)
                add(onBackground)
                add(border)
                if (includeSuccessColors) {
                    add(successBackgroundColor)
                    add(onSuccessBackgroundColor)
                }
            }.toTypedArray(),
        )
    }

    internal class ShapeDefinitions(key: String) {
        val cornerRadiusDp = optionalFloat(
            key = "$key.corner_radius_dp",
            displayName = "cornerRadiusDp",
        )
        val borderStrokeWidthDp = optionalFloat(
            key = "$key.border_stroke_width_dp",
            displayName = "borderStrokeWidthDp",
        )
        val heightDp = optionalFloat(
            key = "$key.height_dp",
            displayName = "heightDp",
        )
        val configuration = configuration(
            key = key,
            displayName = "shape",
            children = arrayOf(cornerRadiusDp, borderStrokeWidthDp, heightDp),
        )
    }

    internal class TypographyDefinitions(key: String) {
        val fontResId = font(key = "$key.font_res_id", displayName = "fontResId")
        val fontSizeSp = optionalFloat(
            key = "$key.font_size_sp",
            displayName = "fontSizeSp",
            minimumExclusive = true,
        )
        val configuration = configuration(
            key = key,
            displayName = "typography",
            children = arrayOf(fontResId, fontSizeSp),
        )
    }
}

internal class CheckoutShapesDefinitions(key: String) {
    val cornerRadiusDp = decimal(
        key = "$key.corner_radius_dp",
        displayName = "cornerRadiusDp",
        defaultValue = StripeThemeDefaults.shapes.cornerRadius,
    )
    val borderStrokeWidthDp = decimal(
        key = "$key.border_stroke_width_dp",
        displayName = "borderStrokeWidthDp",
        defaultValue = StripeThemeDefaults.shapes.borderStrokeWidth,
    )
    val bottomSheetCornerRadiusDp = optionalFloat(
        key = "$key.bottom_sheet_corner_radius_dp",
        displayName = "bottomSheetCornerRadiusDp",
    )
    val configuration = configuration(
        key = key,
        displayName = "shapes",
        children = arrayOf(cornerRadiusDp, borderStrokeWidthDp, bottomSheetCornerRadiusDp),
    )
}

internal class CheckoutTypographyDefinitions(key: String) {
    val sizeScaleFactor = decimal(
        key = "$key.size_scale_factor",
        displayName = "sizeScaleFactor",
        defaultValue = StripeThemeDefaults.typography.fontSizeMultiplier,
        minimumExclusive = true,
    )
    val fontResId = font(key = "$key.font_res_id", displayName = "fontResId")
    val custom = CustomDefinitions(key = "$key.custom")
    val configuration = configuration(
        key = key,
        displayName = "typography",
        children = arrayOf(sizeScaleFactor, fontResId, custom.configuration),
    )

    internal class CustomDefinitions(key: String) {
        val h1 = CheckoutFontDefinitions(key = "$key.h1", displayName = "h1")
        val configuration = configuration(
            key = key,
            displayName = "custom",
            children = arrayOf(h1.configuration),
        )
    }
}

/**
 * Maps to a nullable `Typography.Font`. The font is only passed to the SDK when at least one field is set.
 */
internal class CheckoutFontDefinitions(
    key: String,
    displayName: String,
) {
    val fontFamily = font(key = "$key.font_family", displayName = "fontFamily")
    val fontSizeSp = optionalFloat(
        key = "$key.font_size_sp",
        displayName = "fontSizeSp",
        minimumExclusive = true,
    )
    val fontWeight = optionalInt(
        key = "$key.font_weight",
        displayName = "fontWeight",
    )
    val letterSpacingSp = optionalFloat(
        key = "$key.letter_spacing_sp",
        displayName = "letterSpacingSp",
        minimum = Float.NEGATIVE_INFINITY,
    )
    val configuration = configuration(
        key = key,
        displayName = displayName,
        children = arrayOf(fontFamily, fontSizeSp, fontWeight, letterSpacingSp),
    )

    fun isSet(values: CheckoutPlaygroundSettingValues): Boolean {
        return values[fontFamily] != CheckoutFont.Default ||
            values[fontSizeSp] != null ||
            values[fontWeight] != null ||
            values[letterSpacingSp] != null
    }
}

internal class CheckoutInsetsDefinitions(
    key: String,
    displayName: String,
    defaultStartDp: Float,
    defaultTopDp: Float,
    defaultEndDp: Float,
    defaultBottomDp: Float,
) {
    val startDp = decimal(key = "$key.start_dp", displayName = "startDp", defaultValue = defaultStartDp)
    val topDp = decimal(key = "$key.top_dp", displayName = "topDp", defaultValue = defaultTopDp)
    val endDp = decimal(key = "$key.end_dp", displayName = "endDp", defaultValue = defaultEndDp)
    val bottomDp = decimal(key = "$key.bottom_dp", displayName = "bottomDp", defaultValue = defaultBottomDp)
    val configuration = configuration(
        key = key,
        displayName = displayName,
        children = arrayOf(startDp, topDp, endDp, bottomDp),
    )
}

/**
 * Maps to nullable `Insets`. The insets are only passed to the SDK when at least one side is set; unset sides use 0.
 */
internal class CheckoutOptionalInsetsDefinitions(
    key: String,
    displayName: String,
) {
    val startDp = optionalFloat(key = "$key.start_dp", displayName = "startDp")
    val topDp = optionalFloat(key = "$key.top_dp", displayName = "topDp")
    val endDp = optionalFloat(key = "$key.end_dp", displayName = "endDp")
    val bottomDp = optionalFloat(key = "$key.bottom_dp", displayName = "bottomDp")
    val configuration = configuration(
        key = key,
        displayName = displayName,
        children = arrayOf(startDp, topDp, endDp, bottomDp),
    )

    fun isSet(values: CheckoutPlaygroundSettingValues): Boolean {
        return listOf(startDp, topDp, endDp, bottomDp).any { values[it] != null }
    }
}
