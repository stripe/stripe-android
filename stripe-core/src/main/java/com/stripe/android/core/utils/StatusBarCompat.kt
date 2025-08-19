package com.stripe.android.core.utils

import android.app.Activity
import android.os.Build
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object StatusBarCompat {
    @Suppress("DEPRECATION")
    fun color(activity: Activity): Int? {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            null
        } else {
            activity.window?.statusBarColor
        }
    }

    @Suppress("DEPRECATION")
    fun setColor(activity: Activity, color: Int) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.window?.let { window ->
                window.statusBarColor = color
            }
        }
    }
}
