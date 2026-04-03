package com.stripe.android.core.utils

import android.content.Context
import android.content.pm.PackageInfo
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ContextUtils {
    val Context.packageInfo: PackageInfo?
        @JvmSynthetic
        get() = runCatching {
            packageManager.getPackageInfo(packageName, 0)
        }.getOrNull()
}
