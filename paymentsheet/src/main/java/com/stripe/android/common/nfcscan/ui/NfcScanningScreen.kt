package com.stripe.android.common.nfcscan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
    val onClose = {
        viewActionHandler(NfcScanningViewAction.Close)
    }

    when (state) {
        is NfcScanningViewState.Unavailable -> {
            NfcScanningFullScreenErrorLayout(
                message = state.message,
                deviceRotation = deviceRotation,
                onClose = {
                    viewActionHandler(NfcScanningViewAction.Close)
                },
            )
        }
        is NfcScanningViewState.Available -> {
            NfcScanningLayout(
                status = state.status,
                tapZone = state.tapZone,
                deviceRotation = deviceRotation,
                onClose = onClose,
                onSuccessShown = {
                    viewActionHandler(NfcScanningViewAction.SuccessShown)
                },
            )
        }
    }
}

@Composable
internal fun NfcScanningLayout(
    status: NfcScanningStatus,
    tapZone: TapZone,
    deviceRotation: DeviceRotation,
    onClose: () -> Unit,
    onSuccessShown: () -> Unit,
) {
    val tapZone = remember(deviceRotation, tapZone) {
        createNormalizedTapZone(deviceRotation, tapZone)
    }

    val canShowCloseButton = status !is NfcScanningStatus.Scanned

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface),
    ) {
        NfcCoilLayout(status, tapZone, deviceRotation, onSuccessShown)
        CloseButtonLayout(canShowCloseButton, tapZone, deviceRotation, onClose)
    }
}

@Composable
private fun BoxScope.CloseButtonLayout(
    canShow: Boolean,
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
        AnimatedVisibility(
            visible = canShow,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            NfcCloseButton(onClose)
        }
    }
}

private val DefaultEdgePadding = 20.dp
private val BottomCenterEdgePadding = 68.dp
