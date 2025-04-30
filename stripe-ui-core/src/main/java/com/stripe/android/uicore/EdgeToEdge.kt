package com.stripe.android.uicore

import android.app.Activity
import android.os.Build
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Activity.disableNavigationBarContrastEnforcement() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}
