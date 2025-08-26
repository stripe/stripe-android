package com.stripe.android.link.theme

import androidx.compose.material.ContentAlpha
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stripe.android.uicore.SectionStyle
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

// ECE Link Theme Colors
internal val EceLinkWhiteTextPrimary = Neutral900
internal val EceLinkWhiteBackground = Neutral0

internal data class LinkColors(
    val isDark: Boolean,
    val surfacePrimary: Color,
    val surfaceSecondary: Color,
    val surfaceTertiary: Color,
    val surfaceBackdrop: Color,
    val borderDefault: Color,
    val borderSelected: Color,
    val borderCritical: Color,
    val buttonPrimary: Color,
    val buttonTertiary: Color,
    val buttonBrand: Color,
    val buttonCritical: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textWhite: Color,
    val textBrand: Color,
    val textCritical: Color,
    val onButtonPrimary: Color,
    val onButtonBrand: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    val iconTertiary: Color,
    val iconWhite: Color,
    val iconBrand: Color,
    val iconCritical: Color,
    val outline: Color,
)

internal object LinkThemeConfig {
    fun colors(isDark: Boolean): LinkColors {
        return if (isDark) colorsDark else colorsLight
    }

    private val colorsLight = LinkColors(
        isDark = false,
        surfacePrimary = Neutral0,
        surfaceSecondary = Neutral100,
        surfaceTertiary = Neutral200,
        surfaceBackdrop = Neutral900,
        borderDefault = Neutral300,
        borderSelected = Neutral900,
        borderCritical = Critical500,
        buttonPrimary = Brand200,
        buttonTertiary = Neutral0,
        buttonBrand = Brand200,
        buttonCritical = Critical500,
        textPrimary = Neutral900,
        textSecondary = Neutral700,
        textTertiary = Neutral500,
        textWhite = Neutral0,
        textBrand = Brand600,
        onButtonPrimary = Neutral900,
        onButtonBrand = Neutral900,
        textCritical = Critical600,
        iconPrimary = Neutral900,
        iconSecondary = Neutral700,
        iconTertiary = Neutral500,
        iconWhite = Neutral0,
        iconBrand = Brand200,
        iconCritical = Critical500,
        outline = Color(0f, 0f, 0f, .2f),
    )

    private val colorsDark = LinkColors(
        isDark = true,
        surfacePrimary = Neutral900,
        surfaceSecondary = Neutral800,
        surfaceTertiary = Neutral700,
        surfaceBackdrop = Neutral900,
        borderDefault = Neutral900,
        borderSelected = Brand200,
        borderCritical = Critical500,
        buttonPrimary = Brand200,
        buttonTertiary = Neutral800,
        buttonBrand = Brand200,
        buttonCritical = Critical600,
        textPrimary = Neutral0,
        textSecondary = Neutral300,
        textTertiary = Neutral400,
        textWhite = Neutral0,
        textBrand = Brand200,
        textCritical = Critical400,
        onButtonPrimary = Neutral900,
        onButtonBrand = Neutral900,
        iconPrimary = Neutral100,
        iconSecondary = Neutral500,
        iconTertiary = Neutral500,
        iconWhite = Neutral0,
        iconBrand = Brand200,
        iconCritical = Critical500,
        outline = Color(1f, 1f, 1f, .2f),
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
        get() = run {
            val unselectedColor = if (isDark) Neutral700 else LinkTheme.colors.borderDefault
            RadioButtonDefaults.colors(
                selectedColor = LinkTheme.colors.buttonPrimary,
                unselectedColor = unselectedColor,
                disabledColor = unselectedColor.copy(alpha = ContentAlpha.disabled)
            )
        }
}

@Composable
internal fun StripeThemeForLink(
    sectionStyle: SectionStyle = SectionStyle.Borderless,
    content: @Composable () -> Unit
) {
    val stripeDefaultColors = StripeThemeDefaults.colors(isDark = LinkTheme.colors.isDark)

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
            cornerRadius = 12f,
            bottomSheetCornerRadius = 24f,
        ),
        typography = StripeThemeDefaults.typography,
        sectionSpacing = StripeThemeDefaults.sectionSpacing,
        sectionStyle = sectionStyle,
        textFieldInsets = StripeThemeDefaults.textFieldInsets,
        iconStyle = StripeThemeDefaults.iconStyle,
    ) {
        content()
    }
}
