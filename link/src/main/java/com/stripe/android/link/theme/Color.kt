package com.stripe.android.link.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import com.stripe.android.link.ui.LinkUi

private val LinkTeal = Color(0xFF33DDB3)
private val LinkTeal2024 = Color(0xFF00D66F)
private val ActionLightGreen = Color(0xFF1AC59B)
private val ActionLightGreen2024 = Color(0xFF00A355)
private val ButtonLabel = Color(0xFF1D3944)
private val ButtonLabel2024 = Color(0xFF011E0F)
private val ErrorText = Color(0xFFFF2F4C)
private val ErrorBackground = Color(0x2EFE87A1)

private val LightTextPrimary = Color(0xFF30313D)
private val LightTextSecondary = Color(0xFF6A7383)
private val LightBackground = Color.White
private val LightFill = Color(0xFFF6F8FA)

private val DarkTextPrimary = Color.White
private val DarkTextSecondary = Color(0x99EBEBF5)
private val DarkBackground = Color(0xFF1C1C1E)
private val DarkFill = Color(0x33787880)

internal data class LinkColors(
    val buttonLabel: Color,
    val actionLabelLight: Color,
    val errorText: Color,
    val errorComponentBackground: Color,
    val materialColors: Colors
)

internal object LinkThemeConfig {
    fun colors(isDark: Boolean): LinkColors {
        return if (isDark) colorsDark else colorsLight
    }

    private val colorsLight = LinkColors(
        buttonLabel = if (LinkUi.useNewBrand) ButtonLabel2024 else ButtonLabel,
        actionLabelLight = if (LinkUi.useNewBrand) ActionLightGreen2024 else ActionLightGreen,
        errorText = ErrorText,
        errorComponentBackground = ErrorBackground,
        materialColors = lightColors(
            primary = if (LinkUi.useNewBrand) LinkTeal2024 else LinkTeal,
            secondary = LightFill,
            background = LightBackground,
            surface = LightBackground,
            onPrimary = LightTextPrimary,
            onSecondary = LightTextSecondary
        )
    )

    private val colorsDark = colorsLight.copy(
        materialColors = darkColors(
            primary = if (LinkUi.useNewBrand) LinkTeal2024 else LinkTeal,
            secondary = DarkFill,
            background = DarkBackground,
            surface = DarkBackground,
            onPrimary = DarkTextPrimary,
            onSecondary = DarkTextSecondary
        )
    )
}
