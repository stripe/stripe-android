package com.stripe.android.identity.ui

import android.content.res.Resources
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import com.google.accompanist.themeadapter.material.createMdcTheme
import com.stripe.android.uicore.LocalColors
import com.stripe.android.uicore.LocalShapes
import com.stripe.android.uicore.LocalTypography
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.StripeTypography
import com.stripe.android.uicore.toComposeTypography
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

    val isRobolectricTest = runCatching {
        Build.FINGERPRINT.lowercase() == "robolectric"
    }.getOrDefault(false)

    val inspectionMode = LocalInspectionMode.current || isRobolectricTest
    val hostingAppColors = themeParams.colors ?: MaterialTheme.colors
    val hostingAppTypography = themeParams.typography ?: MaterialTheme.typography
    val hostingAppShapes = themeParams.shapes ?: MaterialTheme.shapes

    val stripeTypography: StripeTypography = StripeThemeDefaults.typography.copy(
        body1FontFamily = hostingAppTypography.body1.fontFamily,
        body2FontFamily = hostingAppTypography.body2.fontFamily,
        h4FontFamily = hostingAppTypography.h4.fontFamily,
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
private fun Typography.makeCopy(
    h1: TextStyle = this.h1,
    h2: TextStyle = this.h2,
    h3: TextStyle = this.h3,
    h4: TextStyle = this.h4,
    h5: TextStyle = this.h5,
    h6: TextStyle = this.h6,
    subtitle1: TextStyle = this.subtitle1,
    subtitle2: TextStyle = this.subtitle2,
    body1: TextStyle = this.body1,
    body2: TextStyle = this.body2,
    button: TextStyle = this.button,
    caption: TextStyle = this.caption,
    overline: TextStyle = this.overline
): Typography = Typography(
    h1 = h1,
    h2 = h2,
    h3 = h3,
    h4 = h4,
    h5 = h5,
    h6 = h6,
    subtitle1 = subtitle1,
    subtitle2 = subtitle2,
    body1 = body1,
    body2 = body2,
    button = button,
    caption = caption,
    overline = overline
)

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
