package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.stripe.android.common.nfcscan.tapzone.TapZone

internal val CoilCircleSize = 200.dp
private val ShadowElevation = 8.dp

@Composable
internal fun NfcCoilLayout(
    status: NfcScanningStatus,
    tapZone: TapZone,
    deviceRotation: DeviceRotation,
    onSuccessShown: () -> Unit,
) {
    val shouldRenderTextAboveCoil = rememberOrientationValues(
        deviceRotation = deviceRotation,
        onPortrait = { tapZone.yBias > 0.75 },
        onLandscape = { tapZone.yBias > 0.6 },
    )

    val canShowInstructionText = status !is NfcScanningStatus.Scanned

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = BiasAlignment(
                horizontalBias = tapZone.xBias * 2 - 1,
                verticalBias = tapZone.yBias * 2 - 1,
            ),
        ) {
            NfcCoil(
                status = status,
                onSuccessShown = onSuccessShown,
            )
        }

        NfcCoilInstructionTextLayout(
            containerWidth = maxWidth,
            containerHeight = maxHeight,
            tapZone = tapZone,
            shouldRenderTextAboveCoil = shouldRenderTextAboveCoil,
            coilSize = CoilCircleSize,
            canShow = canShowInstructionText,
        )
    }
}

@Composable
private fun NfcCoil(
    status: NfcScanningStatus,
    onSuccessShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()

    val shadow = if (isDarkTheme) {
        Modifier
    } else {
        Modifier.shadow(elevation = ShadowElevation, shape = CircleShape)
    }

    Box(
        modifier = modifier
            .size(CoilCircleSize)
            .then(shadow)
            .background(color = MaterialTheme.colors.primary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        NfcCoilAnimatedInterior(
            status = status,
            onSuccessShown = onSuccessShown,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
