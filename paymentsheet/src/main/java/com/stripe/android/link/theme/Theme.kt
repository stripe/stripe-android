package com.stripe.android.link.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkAppearance

internal val LocalLinkTypography = staticCompositionLocalOf<LinkTypography> {
    error("No Typography provided")
}

internal val LocalLinkColors = staticCompositionLocalOf<LinkColors> {
    error("No Colors provided")
}

internal val LocalLinkShapes = staticCompositionLocalOf<LinkShapes> {
    error("No Shapes provided")
}

internal val MinimumTouchTargetSize = 48.dp
internal val PrimaryButtonHeight = 56.dp
internal val AppBarHeight = 70.dp
internal val HorizontalPadding = 20.dp

@Composable
internal fun DefaultLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLinkTypography provides linkTypography,
        LocalLinkColors provides LinkThemeConfig.colors(darkTheme),
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

@Composable
internal fun LinkAppearanceTheme(
    appearance: LinkAppearance? = null,
    content: @Composable () -> Unit
) {
    appearance?.let {
        val isDark: Boolean = when (appearance.style) {
            LinkAppearance.Style.ALWAYS_LIGHT -> false
            LinkAppearance.Style.ALWAYS_DARK -> true
            LinkAppearance.Style.AUTOMATIC -> isSystemInDarkTheme()
        }

        val defaultColors = LinkThemeConfig.colors(isDark)
        val overrides = if (isDark) appearance.darkColors else appearance.lightColors
        val resolvedColors = defaultColors.copy(
            textBrand = overrides.primary,
            buttonBrand = overrides.primary,
            borderSelected = overrides.borderSelected
        )

        val baseContext = LocalContext.current

        val styleContext = remember(baseContext, isDark) {
            val config = Configuration(baseContext.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (isDark) {
                        Configuration.UI_MODE_NIGHT_YES
                    } else {
                        Configuration.UI_MODE_NIGHT_NO
                    }
            }
            baseContext.createConfigurationContext(config)
        }

        CompositionLocalProvider(
            LocalLinkColors provides resolvedColors,
            LocalLinkTypography provides linkTypography,
            LocalLinkShapes provides LinkShapes,
            LocalContext provides styleContext
        ) {
            MaterialTheme(
                colors = resolvedColors.toMaterialColors(!isDark),
                content = content
            )
        }
    } ?: run {
        DefaultLinkTheme(
            content = content
        )
    }
}

private fun LinkColors.toMaterialColors(isLight: Boolean): Colors {
    return if (isLight) {
        Colors(
            primary = buttonPrimary,
            primaryVariant = buttonPrimary,
            secondary = buttonTertiary,
            secondaryVariant = buttonTertiary,
            background = surfaceBackdrop,
            surface = surfacePrimary,
            error = buttonCritical,
            onPrimary = textWhite,
            onSecondary = textPrimary,
            onBackground = textPrimary,
            onSurface = textPrimary,
            onError = textWhite,
            isLight = true
        )
    } else {
        Colors(
            primary = buttonPrimary,
            primaryVariant = buttonPrimary,
            secondary = buttonTertiary,
            secondaryVariant = buttonTertiary,
            background = surfaceBackdrop,
            surface = surfacePrimary,
            error = buttonCritical,
            onPrimary = textWhite,
            onSecondary = textPrimary,
            onBackground = textPrimary,
            onSurface = textPrimary,
            onError = textWhite,
            isLight = false
        )
    }
}
