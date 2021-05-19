package com.stripe.android.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green400 = Color(0xFF3CB043)
private val Green600 = Color(0xFFAEF359)
private val Green800 = Color(0xFF234F1E)

private val Yellow400 = Color(0xFFF6E547)
private val Yellow600 = Color(0xFFF5CF1B)
private val Yellow700 = Color(0xFFF3B711)
private val Yellow800 = Color(0xFFF29F05)

private val Blue200 = Color(0xFF9DA3FA)
private val Blue400 = Color(0xFF4860F7)
private val Blue500 = Color(0xFF0540F2)
private val Blue800 = Color(0xFF001CCF)

private val Red300 = Color(0xFFEA6D7E)
private val Red800 = Color(0xFFD00036)

private val StripeDarkPalette = darkColors(
    primary = Blue200,
    primaryVariant = Green400,
    onPrimary = Color.Black,
    secondary = Yellow400,
    onSecondary = Color.Black,
    onSurface = Color.White,
    onBackground = Color.White,
    error = Red300,
    onError = Color.Black
)

private val StripeLight1Palette = lightColors(
    primary = Blue500,  // text line
    primaryVariant = Green400,
    onPrimary = Green800,
    secondary = Yellow700,
    secondaryVariant = Yellow800,
    onSecondary = Color.Green,
    onSurface = Color.White,
    onBackground = Yellow400, // text color - hint/label
    error = Red800,
    onError = Color.White
)

private val Teal = Color(0xFF0097a7)
private val TealLight = Color(0xFF56c8d8)
private val TealDark = Color(0xFF006978)

private val Purple = Color(0xFF4a148c)
private val PurpleLight = Color(0xFF7c43bd)
private val PurpleDark = Color(0xFF12005e)


private val StripeLight2Palette = lightColors(
    primary = Teal,
    primaryVariant = TealLight,
    onPrimary = Color.White,
    secondary = Purple,
    secondaryVariant = PurpleLight,
    onSecondary = Color.Black,
    onSurface = Color.Black,
    onBackground = Color.Black,
    error = Red800,
    onError = Color.White
)

@Composable
fun StripeTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    colors: Colors? = null,
    content: @Composable () -> Unit
) {
    val myColors = colors ?: if (isDarkTheme) StripeDarkPalette else StripeLight2Palette

    MaterialTheme(
        colors = myColors,
        content = content,
//        typography = JetchatTypography,
//        shapes = JetchatShapes
    )
}
