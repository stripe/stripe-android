package com.stripe.elements_ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.lightColors
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Immutable
data class ElementTypography(
    private val fontWeightNormal: Int,
    private val fontWeightMedium: Int,
    private val fontWeightBold: Int,
    private val fontSizeMultiplier: Float,
    private val xxSmallFontSize: TextUnit,
    private val xSmallFontSize: TextUnit,
    private val smallFontSize: TextUnit,
    private val mediumFontSize: TextUnit,
    private val largeFontSize: TextUnit,
    private val xLargeFontSize: TextUnit,
    // global font overrides, takes precedence over individual font overrides below.
    private val fontFamily: Int?,
    // individual front overrides, only valid when fontFamily is null.
    private val body1FontFamily: FontFamily? = null,
    private val body2FontFamily: FontFamily? = null,
    private val h4FontFamily: FontFamily? = null,
    private val h5FontFamily: FontFamily? = null,
    private val h6FontFamily: FontFamily? = null,
    private val subtitle1FontFamily: FontFamily? = null,
    private val captionFontFamily: FontFamily? = null
) {

    private val globalFontFamily: FontFamily?
        get() = fontFamily?.let { FontFamily(Font(it)) }

    val xLarge: TextStyle = TextStyle.Default.copy(
        fontFamily = globalFontFamily ?: h4FontFamily ?: FontFamily.Default,
        fontSize = (xLargeFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightBold)
    )

    val large: TextStyle = TextStyle.Default.copy(
        fontFamily = globalFontFamily ?: h5FontFamily ?: FontFamily.Default,
        fontSize = (largeFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightMedium),
        letterSpacing = (-0.32).sp
    )

    val medium: TextStyle = TextStyle.Default.copy(
        fontFamily = globalFontFamily ?: body1FontFamily ?: FontFamily.Default,
        fontSize = (mediumFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightNormal)
    )

    val small: TextStyle = TextStyle.Default.copy(
        fontFamily = globalFontFamily ?: h6FontFamily ?: FontFamily.Default,
        fontSize = (smallFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightMedium),
        letterSpacing = (-0.15).sp
    )

    val xSmall: TextStyle = TextStyle.Default.copy(
        fontFamily = globalFontFamily ?: captionFontFamily ?: FontFamily.Default,
        fontSize = (xSmallFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightMedium)
    )

    val xxSmall: TextStyle = TextStyle.Default.copy(
        fontFamily = globalFontFamily ?: body2FontFamily ?: FontFamily.Default,
        fontSize = (xxSmallFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightNormal),
        letterSpacing = (-0.15).sp
    )
}


@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ElementColors(
    val primary: Color,
    val surface: Color,
    val onPrimary: Color,
    val onSurface: Color,
    val subtitle: Color,
    val component: Color,
    val placeholder: Color,
    val error: Color,
    val textCursor: Color,
    val appBarIcon: Color,
    val componentBorder: Color,
    val componentDivider: Color,
    val onComponent: Color,
) {
    fun toMaterialColors(): Colors {
        return lightColors(
            primary = primary,
            surface = surface,
            onSurface = onSurface,
            onPrimary = onPrimary,
            error = error,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val lightElementColors = ElementColors(
    primary = Color(0xFF007AFF),
    surface = Color.White,
    onSurface = Color.Black,
    onPrimary = Color.White,
    error = Color.Red,
    component = Color.White,
    componentBorder = Color(0x33787880),
    componentDivider = Color(0x33787880),
    onComponent = Color.Black,
    subtitle = Color(0x99000000),
    textCursor = Color.Black,
    placeholder = Color(0x993C3C43),
    appBarIcon = Color(0x99000000)
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val darkElementColors = ElementColors(
    primary = Color(0xFF0074D4),
    surface = Color(0xFF2E2E2E),
    onSurface = Color.White,
    onPrimary = Color.Black,
    error = Color.Red,
    component = Color.DarkGray,
    componentBorder = Color(0xFF787880),
    componentDivider = Color(0xFF787880),
    onComponent = Color.White,
    subtitle = Color(0x99FFFFFF),
    textCursor = Color.White,
    placeholder = Color(0x61FFFFFF),
    appBarIcon = Color.White,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalElementColors = staticCompositionLocalOf {
    lightElementColors
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ElementsTheme {
    val colors: ElementColors
        @Composable
        get() = LocalElementColors.current
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ElementsTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (isDark) darkElementColors else lightElementColors

    CompositionLocalProvider(
        LocalElementColors provides colors,
    ) {
        MaterialTheme(
            colors = colors.toMaterialColors(),
        ) {
            content()
        }
    }
}
