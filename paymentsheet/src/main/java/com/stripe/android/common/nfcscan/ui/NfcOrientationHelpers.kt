package com.stripe.android.common.nfcscan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
internal fun <T> rememberOrientationValues(
    deviceRotation: DeviceRotation,
    onPortrait: () -> T,
    onLandscape: () -> T
): T {
    val onPortrait by rememberUpdatedState(onPortrait)
    val onLandscape by rememberUpdatedState(onLandscape)

    return remember(deviceRotation) {
        when (deviceRotation) {
            DeviceRotation.Portrait,
            DeviceRotation.UpsideDown -> onPortrait()
            DeviceRotation.LandscapeLeft,
            DeviceRotation.LandscapeRight -> onLandscape()
        }
    }
}
