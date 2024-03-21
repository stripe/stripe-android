package com.stripe.android.core.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import androidx.annotation.RestrictTo
import androidx.core.app.ComponentActivity

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ContextUtils {
    val Context.packageInfo: PackageInfo?
        @JvmSynthetic
        get() = runCatching {
            packageManager.getPackageInfo(packageName, 0)
        }.getOrNull()

    fun extractActivityFromContext(context: Context): ComponentActivity? {
        var currentContext = context
        if (currentContext is ComponentActivity) {
            return currentContext
        } else {
            while (currentContext is ContextWrapper) {
                if (currentContext is ComponentActivity) {
                    return currentContext
                }
                currentContext = currentContext.baseContext
            }
        }
        return null
    }
}
