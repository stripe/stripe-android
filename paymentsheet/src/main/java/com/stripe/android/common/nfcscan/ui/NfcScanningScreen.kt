package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.common.nfcscan.NfcScanningViewAction
import com.stripe.android.common.nfcscan.NfcScanningViewState
import com.stripe.android.common.nfcscan.tapzone.TapZone

@Composable
internal fun NfcScanningScreen(
    state: NfcScanningViewState,
    viewActionHandler: (NfcScanningViewAction) -> Unit,
) {
    val deviceRotation = rememberDeviceRotation()

    NfcScanningLayout(
        tapZone = state.tapZone,
        deviceRotation = deviceRotation,
        onClose = {
            viewActionHandler(NfcScanningViewAction.Close)
        },
    )
}

@Composable
internal fun NfcScanningLayout(
    tapZone: TapZone,
    deviceRotation: DeviceRotation,
    onClose: () -> Unit,
) {
    val tapZone = rememberNormalizedTapZone(deviceRotation, tapZone)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface),
    ) {
        NfcCoilLayout(tapZone, deviceRotation)
        CloseButtonLayout(tapZone, deviceRotation, onClose)
    }
}

@Composable
private fun NfcCoilLayout(
    tapZone: TapZone,
    deviceRotation: DeviceRotation,
) {
    val shouldRenderTextAboveCoil = rememberOrientationValues(
        deviceRotation = deviceRotation,
        onPortrait = { tapZone.yBias > 0.75 },
        onLandscape = { tapZone.yBias > 0.6 },
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = BiasAlignment(
            horizontalBias = tapZone.xBias * 2 - 1,
            verticalBias = tapZone.yBias * 2 - 1,
        ),
    ) {
        NfcCoil(shouldRenderTextAboveCoil)
    }
}

@Composable
private fun BoxScope.CloseButtonLayout(
    tapZone: TapZone,
    deviceRotation: DeviceRotation,
    onClose: () -> Unit,
) {
    val (alignment, padding) = rememberOrientationValues(
        deviceRotation = deviceRotation,
        onPortrait = {
            if (tapZone.yBias < 0.75) {
                Alignment.BottomCenter to PaddingValues(bottom = BottomCenterEdgePadding)
            } else {
                Alignment.TopStart to PaddingValues(start = DefaultEdgePadding, top = DefaultEdgePadding)
            }
        },
        onLandscape = {
            if (tapZone.xBias < 0.25) {
                Alignment.TopEnd to PaddingValues(end = DefaultEdgePadding, top = DefaultEdgePadding)
            } else {
                Alignment.TopStart to PaddingValues(start = DefaultEdgePadding, top = DefaultEdgePadding)
            }
        }
    )

    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeContent)
            .padding(padding)
            .align(alignment)
    ) {
        NfcCloseButton(onClose)
    }
}

@Composable
private fun <T> rememberOrientationValues(
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

private val DefaultEdgePadding = 20.dp
private val BottomCenterEdgePadding = 68.dp
