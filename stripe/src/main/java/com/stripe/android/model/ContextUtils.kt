package com.stripe.android.model

import android.content.Context
import android.content.pm.PackageInfo

internal object ContextUtils {
    internal val Context.packageInfo: PackageInfo?
        get() = runCatching {
            packageManager.getPackageInfo(packageName, 0)
        }.getOrNull()
}
