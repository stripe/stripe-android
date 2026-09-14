package com.stripe.android.crypto.onramp.ui.theme

import androidx.compose.ui.graphics.Color

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
private val Brand200 = Color(0xFF00D66F)

// Critical Colors
private val Critical600 = Color(0xFFC0123C)
private val Critical500 = Color(0xFFE61947)
private val Critical400 = Color(0xFFFA4A67)

internal data class OnrampColors(
    val borderCritical: Color,
    val borderDefault: Color,
    val borderSelected: Color,
    val buttonBrand: Color,
    val iconPrimary: Color,
    val iconTertiary: Color,
    val iconWhite: Color,
    val onButtonBrand: Color,
    val surfaceBackdrop: Color,
    val surfacePrimary: Color,
    val surfaceSecondary: Color,
    val surfaceTertiary: Color,
    val textBrand: Color,
    val textCritical: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
)

internal object OnrampThemeConfig {
    fun colors(isDark: Boolean): OnrampColors = if (isDark) colorsDark else colorsLight

    private val colorsLight = OnrampColors(
        surfacePrimary = Neutral0,
        surfaceSecondary = Neutral100,
        surfaceTertiary = Neutral200,
        surfaceBackdrop = Neutral900,
        borderDefault = Neutral300,
        borderSelected = Neutral900,
        borderCritical = Critical500,
        buttonBrand = Brand200,
        textPrimary = Neutral900,
        textSecondary = Neutral700,
        textTertiary = Neutral500,
        textBrand = Brand600,
        onButtonBrand = Neutral900,
        textCritical = Critical600,
        iconPrimary = Neutral900,
        iconTertiary = Neutral500,
        iconWhite = Neutral0,
    )

    private val colorsDark = OnrampColors(
        surfacePrimary = Neutral900,
        surfaceSecondary = Neutral800,
        surfaceTertiary = Neutral700,
        surfaceBackdrop = Neutral900,
        borderDefault = Neutral900,
        borderSelected = Brand200,
        borderCritical = Critical500,
        buttonBrand = Brand200,
        textPrimary = Neutral0,
        textSecondary = Neutral300,
        textTertiary = Neutral400,
        textBrand = Brand200,
        textCritical = Critical400,
        onButtonBrand = Neutral900,
        iconPrimary = Neutral100,
        iconTertiary = Neutral500,
        iconWhite = Neutral0,
    )
}
