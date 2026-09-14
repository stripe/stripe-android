package com.stripe.android.crypto.onramp.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.view.ContextThemeWrapper
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

internal val LocalOnrampTypography = staticCompositionLocalOf<OnrampTypography> {
    error("No Typography provided")
}

internal val LocalOnrampColors = staticCompositionLocalOf<OnrampColors> {
    error("No Colors provided")
}

internal val LocalOnrampShapes = staticCompositionLocalOf<OnrampShapes> {
    error("No Shapes provided")
}

@Composable
internal fun DefaultOnrampTheme(
    appearance: LinkAppearance.State?,
    content: @Composable () -> Unit
) {
    val isDark = isOnrampDarkTheme(appearance)

    // Colors
    val defaultColors = OnrampThemeConfig.colors(isDark)
    val resolvedColors = appearance
        ?.let {
            val overrides = if (isDark) it.darkColors else it.lightColors
            defaultColors.copy(
                textBrand = overrides.primary,
                onButtonBrand = overrides.contentOnPrimary,
                buttonBrand = overrides.primary,
                borderSelected = overrides.borderSelected,
            )
        }
        ?: defaultColors

    // Shapes
    val defaultOnrampShapes = OnrampShapes(
        primaryButton = RoundedCornerShape(12.dp),
        primaryButtonHeight = 56.dp,
    )
    val shapes = appearance
        ?.let {
            defaultOnrampShapes.copy(
                primaryButton = it.primaryButton.cornerRadiusDp
                    ?.let { radius -> RoundedCornerShape(radius.dp) }
                    ?: defaultOnrampShapes.primaryButton,
                primaryButtonHeight = it.primaryButton.heightDp?.dp
                    ?: defaultOnrampShapes.primaryButtonHeight,
            )
        }
        ?: defaultOnrampShapes

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
        LocalOnrampColors provides resolvedColors,
        LocalOnrampTypography provides onrampTypography,
        LocalOnrampShapes provides shapes,
    ) {
        MaterialTheme(
            colors = debugColors(Color.Magenta),
            content = content
        )
    }
}

@Composable
internal fun isOnrampDarkTheme(appearance: LinkAppearance.State?): Boolean {
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

private fun Context.withUiMode(uiMode: Int, inspectionMode: Boolean): Context {
    if (uiMode == this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        return this
    }
    val config = Configuration(resources.configuration).apply {
        this.uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or uiMode
    }
    return object : ContextThemeWrapper(this, theme) {
        override fun getResources(): Resources? {
            @Suppress("DEPRECATION")
            if (inspectionMode) {
                // Workaround NPE thrown in BridgeContext#createConfigurationContext() when getting resources.
                val baseResources = this@withUiMode.resources
                return Resources(
                    baseResources.assets,
                    baseResources.displayMetrics,
                    config
                )
            }
            return super.getResources()
        }
    }.apply {
        applyOverrideConfiguration(config)
    }
}

/**
 * A Material [Colors] implementation which sets all colors to [debugColor] to discourage usage of
 * [MaterialTheme.colors] in preference to [OnrampColors].
 */
private fun debugColors(
    debugColor: Color
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

internal object OnrampTheme {

    val typography: OnrampTypography
        @Composable
        get() = LocalOnrampTypography.current

    val colors: OnrampColors
        @Composable
        get() = LocalOnrampColors.current

    val shapes: OnrampShapes
        @Composable
        get() = LocalOnrampShapes.current
}
