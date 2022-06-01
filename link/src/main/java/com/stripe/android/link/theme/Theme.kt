package com.stripe.android.link.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

internal val MinimumTouchTargetSize = 48.dp
internal val AppBarHeight = 56.dp
internal val HorizontalPadding = 20.dp

private val LocalColors = staticCompositionLocalOf { LinkThemeConfig.colors(false) }

@Composable
internal fun DefaultLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = LinkThemeConfig.colors(darkTheme)

    CompositionLocalProvider(LocalColors provides colors) {
        MaterialTheme(
            colors = colors.materialColors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

internal val MaterialTheme.linkColors: LinkColors
    @Composable
    @ReadOnlyComposable
    get() = LocalColors.current
