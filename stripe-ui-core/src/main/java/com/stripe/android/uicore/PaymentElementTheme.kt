package com.stripe.android.uicore

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.ContextThemeWrapper
import androidx.annotation.RestrictTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentElementTheme(
    values: PaymentElementThemeValues,
    content: @Composable () -> Unit,
) {
    val isDark = values.themeMode.isDarkTheme(isSystemInDarkTheme())
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
    ) {
        StripeTheme(
            isDark = isDark,
            colors = if (isDark) values.colorsDark else values.colorsLight,
            shapes = values.shapes,
            typography = values.typography,
            primaryButtonStyle = values.primaryButtonStyle,
            formInsets = values.formInsets,
            sectionSpacing = values.sectionSpacing,
            sectionStyle = StripeThemeDefaults.sectionStyle,
            textFieldInsets = values.textFieldInsets,
            iconStyle = values.iconStyle,
            verticalModeRowPadding = values.verticalModeRowPadding,
            content = content,
        )
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
