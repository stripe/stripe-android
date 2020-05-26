package com.stripe.android.utils

import android.content.Context
import android.content.pm.PackageInfo

internal object ContextUtils {
    internal val Context.packageInfo: PackageInfo?
        @JvmSynthetic
        get() = runCatching {
            packageManager.getPackageInfo(packageName, 0)
        }.getOrNull()
}
