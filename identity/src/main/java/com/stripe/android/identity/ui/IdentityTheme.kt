package com.stripe.android.identity.ui

import android.content.res.Resources
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import com.google.accompanist.themeadapter.material.createMdcTheme
import com.stripe.android.uicore.LocalColors
import com.stripe.android.uicore.LocalShapes
import com.stripe.android.uicore.LocalTypography
import com.stripe.android.uicore.StripeThemeDefaults
import java.lang.reflect.Method

/**
 * IdentityTheme tries to read theme values from hosting app's context. Then adapt the values with
 * CompositionLocalProviders required by StripeTheme.
 */
@Composable
internal fun IdentityTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val key = context.theme.key ?: context.theme

    val layoutDirection = LocalLayoutDirection.current

    val themeParams = remember(key) {
        createMdcTheme(
            context = context,
            layoutDirection = layoutDirection,
            readColors = true,
            readTypography = true,
            readShapes = true,
            setTextColors = false,
            setDefaultFontFamily = false
        )
    }

    val mdcColors = themeParams.colors ?: MaterialTheme.colors

    val isRobolectricTest = runCatching {
        Build.FINGERPRINT.lowercase() == "robolectric"
    }.getOrDefault(false)

    val inspectionMode = LocalInspectionMode.current || isRobolectricTest

    // These LocalProviders are required by StripeTheme, refer to StripeTheme.kt for details
    CompositionLocalProvider(
        LocalColors provides StripeThemeDefaults.colors(isSystemInDarkTheme()).copy(
            component = mdcColors.background,
            componentDivider = mdcColors.onSurface.copy(alpha = DividerAlpha),
            onComponent = mdcColors.onBackground,
            subtitle = mdcColors.onBackground.copy(alpha = ContentAlpha.medium),
            placeholderText = mdcColors.onBackground.copy(alpha = ContentAlpha.medium),
            materialColors = mdcColors,
        ),
        LocalShapes provides StripeThemeDefaults.shapes,
        LocalTypography provides StripeThemeDefaults.typography,
        LocalInspectionMode provides inspectionMode,
    ) {
        MaterialTheme(
            colors = mdcColors,
            typography = themeParams.typography ?: MaterialTheme.typography,
            shapes = themeParams.shapes ?: MaterialTheme.shapes,
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

private var sThemeGetKeyMethodFetched = false
private var sThemeGetKeyMethod: Method? = null
private const val DividerAlpha = 0.12f
