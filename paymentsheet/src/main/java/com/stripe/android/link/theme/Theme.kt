package com.stripe.android.link.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val LocalLinkTypography = staticCompositionLocalOf<LinkTypography> {
    error("No Typography provided")
}

internal val LocalLinkColors = staticCompositionLocalOf<LinkColors> {
    error("No Colors provided")
}

internal val LocalLinkColorsV2 = staticCompositionLocalOf<LinkColorsV2> {
    error("No Colors provided")
}

internal val LocalLinkShapes = staticCompositionLocalOf<LinkShapes> {
    error("No Shapes provided")
}

internal val MinimumTouchTargetSize = 48.dp
internal val PrimaryButtonHeight = 56.dp
internal val AppBarHeight = 56.dp
internal val HorizontalPadding = 20.dp

@Composable
internal fun DefaultLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLinkTypography provides linkTypography,
        LocalLinkColors provides LinkThemeConfig.colors(darkTheme),
        LocalLinkColorsV2 provides LinkThemeConfig.colorsV2(darkTheme),
        LocalLinkShapes provides LinkShapes,
    ) {
        MaterialTheme(
            colors = debugColors(),
        ) {
            content()
        }
    }
}

/**
 * A Material [Colors] implementation which sets all colors to [debugColor] to discourage usage of
 * [MaterialTheme.colors] in preference to [FinancialConnectionsColors].
 */
private fun debugColors(
    debugColor: Color = Color.Magenta
) = Colors(
    primary = debugColor,
    primaryVariant = debugColor,
    secondary = debugColor,
    secondaryVariant = debugColor,
    background = debugColor,
    surface = debugColor,
    error = debugColor,
    onPrimary = debugColor,
    onSecondary = debugColor,
    onBackground = debugColor,
    onSurface = debugColor,
    onError = debugColor,
    isLight = true
)
