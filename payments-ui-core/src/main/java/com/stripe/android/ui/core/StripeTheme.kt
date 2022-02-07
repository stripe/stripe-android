package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val Green400 = Color(0xFF3CB043)
private val Green800 = Color(0xFF234F1E)

private val Yellow400 = Color(0xFFF6E547)
private val Yellow700 = Color(0xFFF3B711)
private val Yellow800 = Color(0xFFF29F05)

private val Blue200 = Color(0xFF9DA3FA)
private val Blue500 = Color(0xFF0540F2)

private val Red300 = Color(0xFFEA6D7E)
private val Red800 = Color(0xFFD00036)

private val Teal = Color(0xFF0097a7)
private val TealLight = Color(0xFF56c8d8)

private val Purple = Color(0xFF4a148c)
private val PurpleLight = Color(0xFF7c43bd)

internal val GrayLight = Color(0xFFF8F8F8)

private val StripeDarkPalette = darkColors(
    primary = Blue200,
    primaryVariant = Green400,
    onPrimary = Color.Green,
    secondary = Color.Gray,
    surface = Color.Black,
    onSecondary = Color.Black,
    onSurface = Color.Gray,
    onBackground = Color.Green,
    error = Red300,
    onError = Color.Black
)

private val StripeLightPalette = lightColors(
    primary = Color(0xFF1A1A1A),
    primaryVariant = TealLight,
    onPrimary = Color.Black,
    secondary = Color.Gray,
    secondaryVariant = PurpleLight,
    surface = Color.White,
    onSecondary = Color.Black,
    onSurface = Color.Black,
    onBackground = Color.Black,
    error = Red800,
    onError = Color.White
)

internal val LocalFieldTextStyle = TextStyle.Default.copy(
    fontFamily = FontFamily.SansSerif,
    fontSize = 14.sp
)

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun StripeTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val myColors = if (isDarkTheme) StripeDarkPalette else StripeLightPalette

    MaterialTheme(
        colors = myColors,
        typography = MaterialTheme.typography.copy(
            body1 = LocalFieldTextStyle,
            subtitle1 = LocalFieldTextStyle
        ),
        content = content
    )
}
