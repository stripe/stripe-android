package com.stripe.android.link.theme

import android.content.res.Configuration
import androidx.appcompat.view.ContextThemeWrapper
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
import com.stripe.android.link.ui.image.LocalStripeImageLoader
import com.stripe.android.uicore.image.StripeImageLoader

internal val LocalLinkAppearance = staticCompositionLocalOf<LinkAppearance?> { null }

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
    appearance: LinkAppearance? = LocalLinkAppearance.current,
    content: @Composable () -> Unit
) {
    val stripeImageLoader = runCatching { LocalStripeImageLoader.current }
        .getOrElse { StripeImageLoader(LocalContext.current) }
    val isDark = when (appearance?.style) {
        LinkAppearance.Style.ALWAYS_LIGHT -> false
        LinkAppearance.Style.ALWAYS_DARK -> true
        LinkAppearance.Style.AUTOMATIC, null -> isSystemInDarkTheme()
    }
    val defaultColors = LinkThemeConfig.colors(isDark)
    val resolvedColors = appearance
        ?.let {
            val overrides = if (isDark) appearance.darkColors else appearance.lightColors
            defaultColors.copy(
                textBrand = overrides.primary,
                onButtonPrimary = overrides.contentOnPrimary,
                buttonPrimary = overrides.primary,
                borderSelected = overrides.borderSelected
            )
        }
        ?: defaultColors

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
        ContextThemeWrapper(baseContext, baseContext.theme).apply {
            applyOverrideConfiguration(config)
        }
    }

    CompositionLocalProvider(
        LocalContext provides styleContext,
        LocalLinkColors provides resolvedColors,
        LocalLinkTypography provides linkTypography,
        LocalLinkShapes provides LinkShapes,
        LocalStripeImageLoader provides stripeImageLoader,
    ) {
        MaterialTheme(
            colors = debugColors(),
            content = content
        )
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
