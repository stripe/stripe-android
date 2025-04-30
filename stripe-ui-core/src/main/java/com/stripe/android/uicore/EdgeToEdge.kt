package com.stripe.android.uicore

import android.app.Activity
import android.os.Build

fun Activity.disableNavigationBarContrastEnforcement() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}
