package com.stripe.android.link.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults

// Neutral Colors
private val Neutral900 = Color(0xFF171717)
private val Neutral800 = Color(0xFF262626)
private val Neutral700 = Color(0xFF404040)
private val Neutral500 = Color(0xFF707070)
private val Neutral400 = Color(0xFFA3A3A3)
private val Neutral300 = Color(0xFFD4D4D4)
private val Neutral200 = Color(0xFFE5E5E5)
private val Neutral100 = Color(0xFFF5F5F5)
private val Neutral0 = Color(0xFFFFFFFF)

// Brand Colors
private val Brand600 = Color(0xFF006635)
private val Brand400 = Color(0xFF00A355)
private val Brand200 = Color(0xFF00D66F)

// Critical Colors
private val Critical600 = Color(0xFFC0123C)
private val Critical500 = Color(0xFFE61947)
private val Critical400 = Color(0xFFFA4A67)

internal data class LinkColors(
    val surfacePrimary: Color,
    val surfaceSecondary: Color,
    val surfaceTertiary: Color,
    val surfaceBackdrop: Color,
    val borderDefault: Color,
    val borderSelected: Color,
    val borderCritical: Color,
    val buttonPrimary: Color,
    val buttonSecondary: Color,
    val buttonTertiary: Color,
    val buttonBrand: Color,
    val buttonCritical: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textWhite: Color,
    val textBrand: Color,
    val textCritical: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    val iconTertiary: Color,
    val iconWhite: Color,
    val iconBrand: Color,
    val iconCritical: Color,
)

internal object LinkThemeConfig {
    fun colors(isDark: Boolean): LinkColors {
        return if (isDark) colorsDark else colorsLight
    }

    private val colorsLight = LinkColors(
        surfacePrimary = Neutral0,
        surfaceSecondary = Neutral100,
        surfaceTertiary = Neutral200,
        surfaceBackdrop = Neutral900,
        borderDefault = Neutral300,
        borderSelected = Neutral900,
        borderCritical = Critical500,
        buttonPrimary = Neutral900,
        buttonSecondary = Neutral100,
        buttonTertiary = Neutral0,
        buttonBrand = Brand200,
        buttonCritical = Critical500,
        textPrimary = Neutral900,
        textSecondary = Neutral700,
        textTertiary = Neutral500,
        textWhite = Neutral0,
        textBrand = Brand600,
        textCritical = Critical600,
        iconPrimary = Neutral900,
        iconSecondary = Neutral700,
        iconTertiary = Neutral500,
        iconWhite = Neutral0,
        iconBrand = Brand200,
        iconCritical = Critical500
    )

    private val colorsDark = LinkColors(
        surfacePrimary = Neutral900,
        surfaceSecondary = Neutral800,
        surfaceTertiary = Neutral700,
        surfaceBackdrop = Neutral900,
        borderDefault = Neutral900,
        borderSelected = Brand200,
        borderCritical = Critical500,
        buttonPrimary = Neutral200,
        buttonSecondary = Neutral700,
        buttonTertiary = Neutral800,
        buttonBrand = Brand200,
        buttonCritical = Critical600,
        textPrimary = Neutral0,
        textSecondary = Neutral300,
        textTertiary = Neutral400,
        textWhite = Neutral0,
        textBrand = Brand200,
        textCritical = Critical400,
        iconPrimary = Neutral100,
        iconSecondary = Neutral500,
        iconTertiary = Neutral500,
        iconWhite = Neutral0,
        iconBrand = Brand200,
        iconCritical = Critical500
    )

    /**
     * Workaround:
     *
     * - The new Link theme primary button uses white on dark mode and dark on light mode
     * - But we're still using Link green theming for buttons, regardless of dark mode
     * - This means that the fixed button color is not consistent with variable text / divider colors,
     *   so we need to keep them fixed until we migrate to the updated primary color styling.
     */
    internal val LinkColors.contentOnPrimaryButton
        get() = Neutral900
    internal val LinkColors.separatorOnPrimaryButton
        get() = Brand400

    /**
     * Workaround:
     *
     * Border color doesn't look great for radio buttons on dark mode. We give it a clearer
     * color here.
     *
     */
    internal val LinkColors.radioButtonColors
        @Composable
        get() = RadioButtonDefaults.colors(
            selectedColor = LinkTheme.colors.buttonBrand,
            unselectedColor = if (isSystemInDarkTheme()) Neutral700 else LinkTheme.colors.borderDefault
        )

    /**
     * Workaround:
     *
     * The OTP background color not consistent across light and dark mode.
     */
    internal val LinkColors.otpSurface
        @Composable
        get() = if (isSystemInDarkTheme()) LinkTheme.colors.surfaceSecondary else LinkTheme.colors.surfacePrimary
}

@Composable
internal fun StripeThemeForLink(
    content: @Composable () -> Unit
) {
    val stripeDefaultColors = StripeThemeDefaults.colors(isSystemInDarkTheme())

    StripeTheme(
        colors = stripeDefaultColors.copy(
            component = LinkTheme.colors.surfaceSecondary,
            onComponent = LinkTheme.colors.textPrimary,
            placeholderText = LinkTheme.colors.textTertiary,
            componentDivider = LinkTheme.colors.borderDefault,
            componentBorder = LinkTheme.colors.surfaceSecondary,
            materialColors = stripeDefaultColors.materialColors.copy(
                primary = LinkTheme.colors.borderSelected,
                error = LinkTheme.colors.textCritical,
            )
        ),
        shapes = StripeThemeDefaults.shapes.copy(
            cornerRadius = 9f
        ),
        typography = StripeThemeDefaults.typography
    ) {
        content()
    }
}
