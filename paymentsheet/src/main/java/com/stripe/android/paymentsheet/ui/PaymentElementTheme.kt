package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.ContextThemeWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.LocalStripeThemeIsDarkOverride
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.isSystemDarkTheme

@Composable
internal fun PaymentElementTheme(
    appearance: PaymentSheet.Appearance,
    content: @Composable () -> Unit,
) {
    val isDark = appearance.isDarkTheme()
    val baseContext = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    val styleContext = remember(baseContext, isDark, inspectionMode) {
        baseContext.withUiMode(
            uiMode = if (isDark) {
                Configuration.UI_MODE_NIGHT_YES
            } else {
                Configuration.UI_MODE_NIGHT_NO
            },
            inspectionMode = inspectionMode,
        )
    }

    CompositionLocalProvider(
        LocalContext provides styleContext,
        LocalStripeThemeIsDarkOverride provides isDark,
    ) {
        StripeTheme(colors = StripeTheme.getColors(isDark)) {
            content()
        }
    }
}

@Composable
internal fun PaymentSheet.Appearance.isDarkTheme(): Boolean {
    return themeMode.isDarkTheme(isSystemInDarkTheme())
}

internal fun PaymentSheet.Appearance.isDarkTheme(context: Context): Boolean {
    return themeMode.isDarkTheme(context.isSystemDarkTheme())
}

internal fun PaymentSheet.ThemeMode.isDarkTheme(isSystemDark: Boolean): Boolean {
    return when (this) {
        PaymentSheet.ThemeMode.Automatic -> isSystemDark
        PaymentSheet.ThemeMode.AlwaysLight -> false
        PaymentSheet.ThemeMode.AlwaysDark -> true
    }
}

private fun Context.withUiMode(
    uiMode: Int,
    inspectionMode: Boolean,
): Context {
    if (uiMode == (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)) {
        return this
    }

    val config = Configuration(resources.configuration).apply {
        this.uiMode = (this.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or uiMode
    }

    return object : ContextThemeWrapper(this, theme) {
        override fun getResources(): Resources {
            @Suppress("DEPRECATION")
            if (inspectionMode) {
                val baseResources = this@withUiMode.resources
                return Resources(
                    baseResources.assets,
                    baseResources.displayMetrics,
                    config,
                )
            }

            return super.getResources()
        }
    }.apply {
        applyOverrideConfiguration(config)
    }
}
