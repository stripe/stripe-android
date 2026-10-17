package com.stripe.android.common.nfcscan.ui

import android.content.Context
import android.os.Build
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

internal enum class DeviceRotation {
    Portrait,
    LandscapeLeft,
    LandscapeRight,
    UpsideDown
}

@Composable
internal fun rememberDeviceRotation(): DeviceRotation {
    val context = LocalContext.current
    val view = LocalView.current

    var deviceRotation by remember { mutableStateOf(context.getDeviceRotation()) }

    SideEffect {
        deviceRotation = context.getDeviceRotation()
    }

    DisposableEffect(view) {
        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            deviceRotation = context.getDeviceRotation()
        }

        view.addOnLayoutChangeListener(listener)

        onDispose {
            view.removeOnLayoutChangeListener(listener)
        }
    }

    return deviceRotation
}

internal fun Context.getDeviceRotation(): DeviceRotation {
    @Suppress("DEPRECATION")
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display
    } else {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay
    }

    return when (display?.rotation) {
        Surface.ROTATION_0 -> DeviceRotation.Portrait
        Surface.ROTATION_90 -> DeviceRotation.LandscapeLeft
        Surface.ROTATION_180 -> DeviceRotation.UpsideDown
        Surface.ROTATION_270 -> DeviceRotation.LandscapeRight
        else -> DeviceRotation.Portrait
    }
}
