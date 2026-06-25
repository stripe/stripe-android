package com.stripe.android.common.nfcscan.ui

import android.content.Context
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

internal enum class DeviceRotation {
    Portrait,
    LandscapeLeft,
    LandscapeRight,
    UpsideDown
}

@Composable
internal fun rememberDeviceRotation(): DeviceRotation {
    val context = LocalContext.current

    return remember(context) {
        @Suppress("DEPRECATION")
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay
        }

        when (display?.rotation) {
            Surface.ROTATION_0 -> DeviceRotation.Portrait
            Surface.ROTATION_90 -> DeviceRotation.LandscapeLeft
            Surface.ROTATION_180 -> DeviceRotation.UpsideDown
            Surface.ROTATION_270 -> DeviceRotation.LandscapeRight
            else -> DeviceRotation.Portrait
        }
    }
}
