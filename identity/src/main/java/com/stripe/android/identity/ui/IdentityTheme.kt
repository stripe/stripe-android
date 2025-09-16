package com.stripe.android.identity.ui

import android.content.res.Resources
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.core.content.res.use
import com.google.accompanist.themeadapter.material.createMdcTheme
import com.stripe.android.uicore.LocalColors
import com.stripe.android.uicore.LocalSectionStyle
import com.stripe.android.uicore.LocalShapes
import com.stripe.android.uicore.LocalTextFieldInsets
import com.stripe.android.uicore.LocalTypography
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.StripeTypography
import com.stripe.android.uicore.toComposeTypography
import java.lang.reflect.Method
import com.google.accompanist.themeadapter.material.R as MaterialR
import com.google.accompanist.themeadapter.material3.R as Material3R

/**
 * IdentityTheme tries to read theme values from hosting app's context. Then adapt the values with
 * CompositionLocalProviders required by StripeTheme.
 */
@Composable
internal fun IdentityTheme(content: @Composable () -> Unit) {
    val themeParameters = extractThemeParameters()

    val isRobolectricTest = runCatching {
        Build.FINGERPRINT.lowercase() == "robolectric"
    }.getOrDefault(false)

    val inspectionMode = LocalInspectionMode.current || isRobolectricTest

    AdoptForStripeTheme(
        hostingAppColors = themeParameters.colors,
        hostingAppTypography = themeParameters.typography,
        hostingAppShapes = themeParameters.shapes,
        inspectionMode = inspectionMode,
        content = content
    )
}

/**
 * Extracts theme parameters from the hosting app's context
 */
@Composable
private fun extractThemeParameters(): ThemeParameters {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val key = context.theme.key ?: context.theme

    val defaultParameters = ThemeParameters(
        colors = MaterialTheme.colors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    )

    val themeType = detectHostThemeType()

    return remember(key, themeType) {
        when (themeType) {
            HostThemeType.MaterialComponents -> {
                runCatching {
                    val themeParams = createMdcTheme(
                        context = context,
                        layoutDirection = layoutDirection,
                        readColors = true,
                        readTypography = true,
                        readShapes = true,
                        setTextColors = false,
                        setDefaultFontFamily = false
                    )
                    ThemeParameters(
                        colors = themeParams.colors ?: defaultParameters.colors,
                        typography = themeParams.typography ?: defaultParameters.typography,
                        shapes = themeParams.shapes ?: defaultParameters.shapes
                    )
                }.getOrElse {
                    defaultParameters
                }
            }
            HostThemeType.Material3,
            HostThemeType.Unknown -> defaultParameters
        }
    }
}

@Composable
internal fun AdoptForStripeTheme(
    hostingAppColors: Colors,
    hostingAppTypography: Typography,
    hostingAppShapes: Shapes,
    inspectionMode: Boolean,
    content: @Composable () -> Unit
) {
    val stripeTypography: StripeTypography = StripeThemeDefaults.typography.copy(
        body1FontFamily = hostingAppTypography.body1.fontFamily,
        body2FontFamily = hostingAppTypography.body2.fontFamily,
        h4 = TextStyle(
            fontFamily = hostingAppTypography.h4.fontFamily,
        ),
        h5FontFamily = hostingAppTypography.h5.fontFamily,
        h6FontFamily = hostingAppTypography.h6.fontFamily,
        subtitle1FontFamily = hostingAppTypography.subtitle1.fontFamily,
        captionFontFamily = hostingAppTypography.caption.fontFamily,
    )
    // These LocalProviders are required by StripeTheme, refer to StripeTheme.kt for details
    CompositionLocalProvider(
        LocalColors provides StripeThemeDefaults.colors(isSystemInDarkTheme()).copy(
            component = hostingAppColors.background,
            componentDivider = hostingAppColors.onSurface.copy(alpha = DividerAlpha),
            onComponent = hostingAppColors.onBackground,
            subtitle = hostingAppColors.onBackground.copy(alpha = ContentAlpha.medium),
            placeholderText = hostingAppColors.onBackground.copy(alpha = ContentAlpha.medium),
            materialColors = hostingAppColors,
        ),
        LocalShapes provides StripeThemeDefaults.shapes,
        LocalTypography provides stripeTypography,
        LocalInspectionMode provides inspectionMode,
        LocalSectionStyle provides StripeThemeDefaults.sectionStyle,
        LocalTextFieldInsets provides StripeThemeDefaults.textFieldInsets,
    ) {
        MaterialTheme(
            colors = hostingAppColors,
            // stripeTypography.toComposeTypography has overridden body1, body2, h4, h5, h6, subtitle1, caption
            // need to copy over the rest from hosting app.
            typography = stripeTypography.toComposeTypography().copy(
                h1 = hostingAppTypography.h1,
                h2 = hostingAppTypography.h2,
                h3 = hostingAppTypography.h3,
                subtitle2 = hostingAppTypography.subtitle2,
                button = hostingAppTypography.button,
                overline = hostingAppTypography.overline
            ),
            shapes = hostingAppShapes,
            content = content
        )
    }
}

/**
 * Copied from MdcTheme.kt
 */
private inline val Resources.Theme.key: Any?
    get() {
        if (!sThemeGetKeyMethodFetched) {
            try {
                @Suppress("SoonBlockedPrivateApi")
                sThemeGetKeyMethod = Resources.Theme::class.java.getDeclaredMethod("getKey")
                    .apply { isAccessible = true }
            } catch (e: ReflectiveOperationException) {
                // Failed to retrieve Theme.getKey method
            }
            sThemeGetKeyMethodFetched = true
        }
        if (sThemeGetKeyMethod != null) {
            return try {
                sThemeGetKeyMethod?.invoke(this)
            } catch (e: ReflectiveOperationException) {
                // Failed to invoke Theme.getKey()
            }
        }
        return null
    }

@Composable
private fun detectHostThemeType(): HostThemeType {
    val context = LocalContext.current

    val isMaterialTheme = remember {
        runCatching {
            context.obtainStyledAttributes(MaterialR.styleable.ThemeAdapterMaterialTheme).use { ta ->
                ta.hasValue(MaterialR.styleable.ThemeAdapterMaterialTheme_isMaterialTheme)
            }
        }.getOrDefault(false)
    }

    val isMaterial3Theme = remember {
        runCatching {
            context.obtainStyledAttributes(Material3R.styleable.ThemeAdapterMaterial3Theme).use { ta ->
                ta.hasValue(Material3R.styleable.ThemeAdapterMaterial3Theme_isMaterial3Theme)
            }
        }.getOrDefault(false)
    }

    return when {
        isMaterialTheme -> HostThemeType.MaterialComponents
        isMaterial3Theme -> HostThemeType.Material3
        else -> HostThemeType.Unknown
    }
}

private enum class HostThemeType {
    MaterialComponents,
    Material3,
    Unknown
}

private data class ThemeParameters(
    val colors: Colors,
    val typography: Typography,
    val shapes: Shapes
)

private var sThemeGetKeyMethodFetched = false
private var sThemeGetKeyMethod: Method? = null
internal const val DividerAlpha = 0.12f
