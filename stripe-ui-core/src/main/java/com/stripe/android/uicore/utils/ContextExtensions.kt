package com.stripe.android.uicore.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.ContextThemeWrapper
import androidx.annotation.RestrictTo

/**
 * Applies a night-mode override to a local context while preserving the supplied configuration.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Context.withUiMode(
    configuration: Configuration,
    isDark: Boolean,
    inspectionMode: Boolean,
): Context {
    val config = Configuration(configuration).apply {
        val nightMode = if (isDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
    }
    if (config == resources.configuration) {
        return this
    }

    return object : ContextThemeWrapper(this, theme) {
        override fun getResources(): Resources {
            @Suppress("DEPRECATION")
            if (inspectionMode) {
                // Layoutlib's BridgeContext does not support createConfigurationContext().
                val baseResources = this@withUiMode.resources
                return Resources(baseResources.assets, baseResources.displayMetrics, config)
            }
            return super.getResources()
        }
    }.apply {
        applyOverrideConfiguration(config)
    }
}
