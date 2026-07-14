package com.stripe.android.common.nfcscan.ui

import com.stripe.android.common.nfcscan.tapzone.TapZone

internal fun createNormalizedTapZone(
    rotation: DeviceRotation,
    tapZone: TapZone
): TapZone {
    val xBias = tapZone.xBias
    val yBias = tapZone.yBias

    val (normalizedXBias, normalizedYBias) = when (rotation) {
        DeviceRotation.Portrait -> xBias to yBias
        DeviceRotation.UpsideDown -> 1 - xBias to 1 - yBias
        DeviceRotation.LandscapeLeft -> yBias to (1 - xBias)
        DeviceRotation.LandscapeRight -> (1 - yBias) to xBias
    }

    return TapZone(
        xBias = normalizedXBias,
        yBias = normalizedYBias,
    )
}
