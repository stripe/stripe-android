package com.stripe.android.link.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorPalette = darkColors(
    primary = LinkGreen,
    secondary = LinkLightGray,
    background = Color.Black,
    onPrimary = Color.Black,
    onSecondary = LinkDarkGray,
    onBackground = LinkGray
)

private val LightColorPalette = lightColors(
    primary = LinkGreen,
    secondary = LinkLightGray,
    background = Color.White,
    onPrimary = Color.Black,
    onSecondary = LinkDarkGray,
    onBackground = LinkGray
)

internal val CloseIconWidth = 24.dp
internal val AppBarHeight = 56.dp
internal val HorizontalPadding = 20.dp

@Composable
internal fun linkTextFieldColors() =
    TextFieldDefaults.textFieldColors(
        backgroundColor = MaterialTheme.colors.background,
        focusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

@Composable
internal fun DefaultLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
