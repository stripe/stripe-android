package com.stripe.android.link.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

private val LinkGreen = Color(0xFF33DDB3)
private val ButtonLabel = Color(0xFF1D3944)

private val LightTextPrimary = Color(0xFF30313D)
private val LightTextSecondary = Color(0xFF6A7383)
private val LightTextDisabled = Color(0xFFA3ACBA)
private val LightBackground = Color.White
private val LightFill = Color(0xFFF6F8FA)

private val DarkTextPrimary = Color.White
private val DarkTextSecondary = Color(0x99EBEBF5)
private val DarkTextDisabled = Color(0x61FFFFFF)
private val DarkBackground = Color(0xFF1C1C1E)
private val DarkFill = Color(0x33787880)

internal data class LinkColors(
    val componentBackground: Color,
    val componentBorder: Color,
    val componentDivider: Color,
    val buttonLabel: Color,
    val disabledText: Color,
    val materialColors: Colors
)

internal object LinkThemeConfig {
    fun colors(isDark: Boolean): LinkColors {
        return if (isDark) colorsDark else colorsLight
    }

    private val colorsLight = LinkColors(
        componentBackground = Color.White,
        componentBorder = Color(0xFFE0E6EB),
        componentDivider = Color(0xFFEFF2F4),
        buttonLabel = ButtonLabel,
        disabledText = LightTextDisabled,
        materialColors = lightColors(
            primary = LinkGreen,
            secondary = LightFill,
            background = LightBackground,
            surface = LightBackground,
            onPrimary = LightTextPrimary,
            onSecondary = LightTextSecondary
        )
    )

    private val colorsDark = LinkColors(
        componentBackground = Color(0x2E747480),
        componentBorder = Color(0x5C787880),
        componentDivider = Color(0x33787880),
        buttonLabel = ButtonLabel,
        disabledText = DarkTextDisabled,
        materialColors = darkColors(
            primary = LinkGreen,
            secondary = DarkFill,
            background = DarkBackground,
            surface = DarkBackground,
            onPrimary = DarkTextPrimary,
            onSecondary = DarkTextSecondary
        )
    )
}
