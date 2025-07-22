package com.stripe.android.core.utils

import android.app.Activity
import android.os.Build
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object StatusBarCompat {
    @Suppress("DEPRECATION")
    fun color(activity: Activity): Int? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            null
        } else {
            activity.window?.statusBarColor
        }
    }
}
