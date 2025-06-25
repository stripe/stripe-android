package com.stripe.android.link.theme

import androidx.compose.runtime.Composable
import com.stripe.android.link.theme.LinkThemeConfig.contentOnPrimaryButton
import com.stripe.android.paymentsheet.PaymentSheet

internal object LinkTheme {

    val typography: LinkTypography
        @Composable
        get() = LocalLinkTypography.current

    val colors: LinkColors
        @Composable
        get() = LocalLinkColors.current

    val shapes: LinkShapes
        @Composable
        get() = LocalLinkShapes.current
}

internal fun LinkTheme.toAddressElementAppearance(): PaymentSheet.Appearance {
    val linkColorsLight = LinkThemeConfig.colors(isDark = false)
    val linkColorsDark = LinkThemeConfig.colors(isDark = true)

    val colorsLight = PaymentSheet.Colors(
        primary = linkColorsLight.borderSelected,
        surface = linkColorsLight.surfacePrimary,
        component = linkColorsLight.surfaceSecondary,
        componentBorder = linkColorsLight.surfaceSecondary,
        componentDivider = linkColorsLight.borderDefault,
        onComponent = linkColorsLight.textPrimary,
        subtitle = linkColorsLight.textSecondary, // TODO
        placeholderText = linkColorsLight.textTertiary,
        onSurface = linkColorsLight.textPrimary,
        appBarIcon = linkColorsLight.iconTertiary,
        error = linkColorsLight.textCritical,
    )

    val colorsDark = PaymentSheet.Colors(
        primary = linkColorsDark.borderSelected,
        surface = linkColorsDark.surfacePrimary,
        component = linkColorsDark.surfaceSecondary,
        componentBorder = linkColorsDark.surfaceSecondary,
        componentDivider = linkColorsDark.borderDefault,
        onComponent = linkColorsDark.textPrimary,
        subtitle = linkColorsDark.textSecondary, // TODO
        placeholderText = linkColorsDark.textTertiary,
        onSurface = linkColorsDark.textPrimary,
        appBarIcon = linkColorsDark.iconTertiary,
        error = linkColorsDark.textCritical,
    )

    val primaryButtonColors = PaymentSheet.PrimaryButtonColors(
        background = linkColorsLight.buttonBrand,
        onBackground = linkColorsLight.contentOnPrimaryButton,
        border = linkColorsLight.buttonBrand,
    )

    return PaymentSheet.Appearance.Builder()
        .colorsLight(colorsLight)
        .colorsDark(colorsDark)
        .shapes(
            PaymentSheet.Shapes(
                cornerRadiusDp = 12f,
                borderStrokeWidthDp = 1.5f,
            )
        )
        .primaryButton(
            PaymentSheet.PrimaryButton(
                shape = PaymentSheet.PrimaryButtonShape(
                    cornerRadiusDp = 12f,
                    heightDp = PrimaryButtonHeight.value,
                ),
                colorsLight = primaryButtonColors,
                colorsDark = primaryButtonColors,
            )
        )
        .build()
}
