package com.stripe.android.common.nfcscan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.stripe.android.common.nfcscan.tapzone.TapZone

@Composable
internal fun rememberNormalizedTapZone(
    rotation: DeviceRotation,
    tapZone: TapZone
): TapZone {
    return remember(rotation, tapZone) {
        val xBias = tapZone.xBias
        val yBias = tapZone.yBias

        val (normalizedXBias, normalizedYBias) = when (rotation) {
            DeviceRotation.Portrait -> xBias to yBias
            DeviceRotation.UpsideDown -> 1 - xBias to 1 - yBias
            DeviceRotation.LandscapeLeft -> yBias to (1 - xBias)
            DeviceRotation.LandscapeRight -> (1 - yBias) to xBias
        }

        TapZone(
            xBias = normalizedXBias,
            yBias = normalizedYBias,
        )
    }
}
