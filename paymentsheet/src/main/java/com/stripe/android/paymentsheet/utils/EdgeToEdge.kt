package com.stripe.android.paymentsheet.utils

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

internal fun ComponentActivity.renderEdgeToEdge() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return
    } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    } else {
        enableEdgeToEdge()
    }
}
