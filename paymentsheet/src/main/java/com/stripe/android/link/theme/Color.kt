package com.stripe.android.link.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.elements.OTPElementColors

// Neutral Colors
private val Neutral900 = Color(0xFF171717)
private val Neutral800 = Color(0xFF262626)
private val Neutral700 = Color(0xFF404040)
private val Neutral600 = Color(0xFF525252)
private val Neutral500 = Color(0xFF707070)
private val Neutral400 = Color(0xFFA3A3A3)
private val Neutral300 = Color(0xFFD4D4D4)
private val Neutral200 = Color(0xFFE5E5E5)
private val Neutral100 = Color(0xFFF5F5F5)
private val Neutral50 = Color(0xFFFAFAFA)
private val Neutral25 = Color(0xFFFEF4F6)
private val Neutral0 = Color(0xFFFFFFFF)

// Brand Colors
private val Brand900 = Color(0xFF011E0F)
private val Brand800 = Color(0xFF023B1E)
private val Brand700 = Color(0xFF034F28)
private val Brand600 = Color(0xFF006635)
private val Brand500 = Color(0xFF008545)
private val Brand400 = Color(0xFF00A355)
private val Brand300 = Color(0xFF00C767)
private val Brand200 = Color(0xFF00D66F)
private val Brand100 = Color(0xFF5EEE97)
private val Brand50 = Color(0xFFE6FFED)
private val Brand25 = Color(0xFFFEF4F6)

// Critical Colors
private val Critical900 = Color(0xFF4E0322)
private val Critical800 = Color(0xFF76072F)
private val Critical700 = Color(0xFF9B0C36)
private val Critical600 = Color(0xFFC0123C)
private val Critical500 = Color(0xFFE61947)
private val Critical400 = Color(0xFFFAA467)
private val Critical300 = Color(0xFFFA7E91)
private val Critical200 = Color(0xFFFAA9B8)
private val Critical100 = Color(0xFFFBD3DC)
private val Critical50 = Color(0xFFFDE9EE)
private val Critical25 = Color(0xFFFEF4F6)

// Attention Colors
private val Attention900 = Color(0xFF4A0F02)
private val Attention800 = Color(0xFF701B01)
private val Attention700 = Color(0xFF922700)
private val Attention600 = Color(0xFFB13600)
private val Attention500 = Color(0xFFCC4B00)
private val Attention400 = Color(0xFFE46602)
private val Attention300 = Color(0xFFF7870F)
private val Attention200 = Color(0xFFFC4AF4)
private val Attention100 = Color(0xFFFBD992)
private val Attention50 = Color(0xFFFCEEB5)
private val Attention25 = Color(0xFFFEF8C9)

// Info Colors
private val Info900 = Color(0xFF0A2156)
private val Info800 = Color(0xFF0D3485)
private val Info700 = Color(0xFF0B46AD)
private val Info600 = Color(0xFF045AD0)
private val Info500 = Color(0xFF007E29)
private val Info400 = Color(0xFF088EF9)
private val Info300 = Color(0xFF3BABFD)
private val Info200 = Color(0xFF6DC9FC)
private val Info100 = Color(0xFFA7E7FC)
private val Info50 = Color(0xFFCBF5FD)
private val Info25 = Color(0xFFE2FBFE)

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
    val typePrimary: Color,
    val typeSecondary: Color,
    val typeTertiary: Color,
    val typeWhite: Color,
    val typeBrand: Color,
    val typeCritical: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    val iconTertiary: Color,
    val iconWhite: Color,
    val iconBrand: Color,
    val iconCritical: Color,
)

internal object LinkThemeConfig {
    fun colors(isDark: Boolean): LinkColors {
        // TODO(carlosmuvi) dark mode.
        return if (isDark) colorsV2Light else colorsV2Light
    }

    private val colorsV2Light = LinkColors(
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
        typePrimary = Neutral900,
        typeSecondary = Neutral700,
        typeTertiary = Neutral500,
        typeWhite = Neutral0,
        typeBrand = Brand600,
        typeCritical = Critical600,
        iconPrimary = Neutral900,
        iconSecondary = Neutral700,
        iconTertiary = Neutral500,
        iconWhite = Neutral0,
        iconBrand = Brand200,
        iconCritical = Critical500
    )

    internal fun LinkColors.otpElementColors(): OTPElementColors {
        return OTPElementColors(
            selectedBorder = borderSelected,
            placeholder = typePrimary
        )
    }
}

@Composable
internal fun StripeThemeForLink(
    content: @Composable () -> Unit
) {
    val stripeDefaultColors = StripeThemeDefaults.colors(isSystemInDarkTheme())

    StripeTheme(
        colors = stripeDefaultColors.copy(
            materialColors = stripeDefaultColors.materialColors.copy(
                primary = LinkTheme.colorsV2.iconBrand
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
