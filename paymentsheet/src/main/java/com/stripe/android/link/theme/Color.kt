package com.stripe.android.link.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.elements.OTPElementColors

private val LinkTeal = Color(0xFF00D66F)
private val ActionLightGreen = Color(0xFF00A355)
private val ActionGreen = Color(0xFF05A87F)
private val ButtonLabel = Color(0xFF011E0F)
private val ErrorText = Color(0xFFFF2F4C)
private val ErrorBackground = Color(0x2EFE87A1)

private val LightComponentBackground = Color.White
private val LightComponentBorder = Color(0xFFE0E6EB)
private val LightComponentDivider = Color(0xFFEFF2F4)
private val LightTextPrimary = Color(0xFF30313D)
private val LightTextSecondary = Color(0xFF6A7383)
private val LightTextDisabled = Color(0xFFA3ACBA)
private val LightBackground = Color.White
private val LightFill = Color(0xFFF6F8FA)
private val LightProgressIndicator = Color(0xFF1D3944)
private val LightSheetScrim = Color(0x1F0A2348)
private val LightSecondaryButtonLabel = Color(0xFF1D3944)
private val LightCloseButton = Color(0xFF30313D)
private val LightLinkLogo = Color(0xFF1D3944)
private val LightOtpPlaceholder = Color(0xFFEBEEF1)

private val DarkComponentBackground = Color(0x2E747480)
private val DarkComponentBorder = Color(0x5C787880)
private val DarkComponentDivider = Color(0x33787880)
private val DarkTextPrimary = Color.White
private val DarkTextSecondary = Color(0x99EBEBF5)
private val DarkTextDisabled = Color(0x61FFFFFF)
private val DarkBackground = Color(0xFF2E2E2E)
private val DarkFill = Color(0x33787880)
private val DarkCloseButton = Color(0x99EBEBF5)
private val DarkLinkLogo = Color.White
private val DarkProgressIndicator = LinkTeal
private val DarkSecondaryButtonLabel = ActionGreen
private val DarkOtpPlaceholder = Color(0x61FFFFFF)

// new color references
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


internal data class LinkColorsV2(
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

@Deprecated(
    message = "LinkColors is deprecated. Use LinkColorsV2 instead.",
    replaceWith = ReplaceWith("LinkColorsV2")
)
internal data class LinkColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val componentBackground: Color,
    val componentBorder: Color,
    val componentDivider: Color,
    val actionLabel: Color,
    val buttonLabel: Color,
    val actionLabelLight: Color,
    val errorText: Color,
    val disabledText: Color,
    val errorComponentBackground: Color,
    val progressIndicator: Color,
    val secondaryButtonLabel: Color,
    val sheetScrim: Color,
    val closeButton: Color,
    val linkLogo: Color,
    val otpElementColors: OTPElementColors,
)

internal object LinkThemeConfig {
    fun colors(isDark: Boolean): LinkColors {
        return if (isDark) colorsDark else colorsLight
    }

    fun colorsV2(isDark: Boolean): LinkColorsV2 {
        return if (isDark) colorsV2Light else colorsV2Light
    }

    private val colorsV2Light = LinkColorsV2(
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

    private val colorsLight = LinkColors(
        primary = LinkTeal,
        secondary = LightFill,
        background = LightBackground,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        componentBackground = LightComponentBackground,
        componentBorder = LightComponentBorder,
        componentDivider = LightComponentDivider,
        buttonLabel = ButtonLabel,
        actionLabelLight = ActionLightGreen,
        errorText = ErrorText,
        errorComponentBackground = ErrorBackground,
        progressIndicator = LightProgressIndicator,
        secondaryButtonLabel = LightSecondaryButtonLabel,
        sheetScrim = LightSheetScrim,
        linkLogo = LightLinkLogo,
        closeButton = LightCloseButton,
        disabledText = LightTextDisabled,
        otpElementColors = OTPElementColors(
            selectedBorder = LinkTeal,
            placeholder = LightOtpPlaceholder
        ),
        actionLabel = ActionGreen,
    )

    private val colorsDark = colorsLight.copy(
        primary = LinkTeal,
        secondary = DarkFill,
        background = DarkBackground,
        textPrimary = DarkTextPrimary,
        textSecondary = DarkTextSecondary,
        componentBackground = DarkComponentBackground,
        componentBorder = DarkComponentBorder,
        componentDivider = DarkComponentDivider,
        progressIndicator = DarkProgressIndicator,
        linkLogo = DarkLinkLogo,
        closeButton = DarkCloseButton,
        disabledText = DarkTextDisabled,
        secondaryButtonLabel = DarkSecondaryButtonLabel,
        otpElementColors = OTPElementColors(
            selectedBorder = LinkTeal,
            placeholder = DarkOtpPlaceholder
        ),
    )
}

@Composable
internal fun StripeThemeForLink(
    content: @Composable () -> Unit
) {
    val stripeDefaultColors = StripeThemeDefaults.colors(isSystemInDarkTheme())

    StripeTheme(
        colors = stripeDefaultColors.copy(
            materialColors = stripeDefaultColors.materialColors.copy(
                primary = ActionGreen
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
