package com.stripe.android.paymentsheet.utils

import android.app.Activity
import android.os.Build
import androidx.core.view.WindowCompat

internal fun Activity.renderEdgeToEdge() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return
    }

    WindowCompat.setDecorFitsSystemWindows(window, false)
}
