package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.StripeTheme

/**
 * Wraps [StripeTheme] with support for forcing a specific color scheme (light/dark),
 * regardless of the system setting.
 */
@Composable
internal fun PaymentElementTheme(
    colorScheme: PaymentSheet.ColorScheme = PaymentSheet.ColorScheme.System,
    content: @Composable () -> Unit,
) {
    val isDark = when (colorScheme) {
        PaymentSheet.ColorScheme.Light -> false
        PaymentSheet.ColorScheme.Dark -> true
        PaymentSheet.ColorScheme.System -> isSystemInDarkTheme()
    }

    val baseContext = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    val styledContext = remember(baseContext, isDark, inspectionMode) {
        val uiMode = if (isDark) {
            Configuration.UI_MODE_NIGHT_YES
        } else {
            Configuration.UI_MODE_NIGHT_NO
        }
        baseContext.withUiMode(uiMode, inspectionMode)
    }

    CompositionLocalProvider(LocalContext provides styledContext) {
        StripeTheme(colors = StripeTheme.getColors(isDark)) {
            content()
        }
    }
}

/**
 * Creates a [Context] wrapper with the given [uiMode] applied to its configuration,
 * so that Android resources resolve for the correct night mode.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Context.withUiMode(uiMode: Int, inspectionMode: Boolean): Context {
    if (uiMode == this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        return this
    }
    val config = Configuration(resources.configuration).apply {
        this.uiMode = (this.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or uiMode
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
