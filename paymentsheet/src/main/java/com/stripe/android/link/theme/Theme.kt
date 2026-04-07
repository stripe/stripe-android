package com.stripe.android.link.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.ui.image.LocalStripeImageLoader
import com.stripe.android.paymentsheet.ui.withUiMode
import com.stripe.android.uicore.image.StripeImageLoader

internal val LocalLinkAppearance = staticCompositionLocalOf<LinkAppearance.State?> { null }

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
internal val AppBarHeight = 70.dp
internal val HorizontalPadding = 20.dp

@Composable
internal fun DefaultLinkTheme(
    appearance: LinkAppearance.State? = LocalLinkAppearance.current,
    content: @Composable () -> Unit
) {
    val stripeImageLoader = StripeImageLoader(LocalContext.current)
    val isDark = isLinkDarkTheme(appearance)

    // Colors
    val defaultColors = LinkThemeConfig.colors(isDark)
    val resolvedColors = appearance
        ?.let {
            val overrides = if (isDark) it.darkColors else it.lightColors
            defaultColors.copy(
                textBrand = overrides.primary,
                onButtonBrand = overrides.contentOnPrimary,
                buttonBrand = overrides.primary,
                borderSelected = overrides.borderSelected
            )
        }
        ?: defaultColors

    // Shapes
    val defaultLinkShapes = LinkShapes()
    val linkShapes = appearance
        ?.let {
            defaultLinkShapes.copy(
                primaryButton = it.primaryButton.cornerRadiusDp
                    ?.let { radius -> RoundedCornerShape(radius.dp) }
                    ?: defaultLinkShapes.primaryButton,
                primaryButtonHeight = it.primaryButton.heightDp?.dp
                    ?: defaultLinkShapes.primaryButtonHeight,
            )
        }
        ?: defaultLinkShapes

    // Set context configuration so the correct resources are loaded.
    val baseContext = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    val styleContext = remember(baseContext, isDark, inspectionMode) {
        val uiMode =
            if (isDark) {
                Configuration.UI_MODE_NIGHT_YES
            } else {
                Configuration.UI_MODE_NIGHT_NO
            }
        baseContext.withUiMode(uiMode, inspectionMode)
    }

    CompositionLocalProvider(
        LocalContext provides styleContext,
        LocalLinkColors provides resolvedColors,
        LocalLinkTypography provides linkTypography,
        LocalLinkShapes provides linkShapes,
        LocalStripeImageLoader provides stripeImageLoader,
    ) {
        MaterialTheme(
            colors = debugColors(),
            content = content
        )
    }
}

@Composable
internal fun isLinkDarkTheme(appearance: LinkAppearance.State?): Boolean {
    return when (appearance?.style) {
        LinkAppearance.Style.ALWAYS_LIGHT -> false
        LinkAppearance.Style.ALWAYS_DARK -> true
        LinkAppearance.Style.AUTOMATIC, null -> isSystemInDarkTheme()
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
