package com.stripe.android.link.theme

import androidx.annotation.RestrictTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkAppearance
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import com.stripe.android.uicore.image.LocalStripeImageLoader
import com.stripe.android.uicore.utils.withUiMode

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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun DefaultLinkTheme(
    appearance: LinkAppearance.State? = LocalLinkAppearance.current,
    content: @Composable () -> Unit
) {
    val stripeImageLoader = DefaultStripeImageLoader(LocalContext.current)
    val isDark = isLinkDarkTheme(appearance)

    // Colors
    val defaultColors = LinkThemeConfig.colors(isDark)
    val resolvedColors = appearance
        ?.let {
            val overrides = if (isDark) it.darkColors else it.lightColors
            defaultColors.copy(
                textBrand = overrides.primary,
                buttonPrimary = overrides.primary,
                onButtonPrimary = overrides.contentOnPrimary,
                borderSelected = overrides.borderSelected,
                accent = overrides.primary,
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
    val configuration = LocalConfiguration.current
    val inspectionMode = LocalInspectionMode.current
    val styleContext = remember(baseContext, configuration, isDark, inspectionMode) {
        baseContext.withUiMode(configuration, isDark, inspectionMode)
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
    val isSystemInDarkTheme = isSystemInDarkTheme()
    return appearance?.style.isDarkTheme(isSystemInDarkTheme)
}

internal fun LinkAppearance.Style?.isDarkTheme(isSystemDarkTheme: Boolean): Boolean {
    return when (this) {
        LinkAppearance.Style.AUTOMATIC, null -> isSystemDarkTheme
        LinkAppearance.Style.ALWAYS_LIGHT -> false
        LinkAppearance.Style.ALWAYS_DARK -> true
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
