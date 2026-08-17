package com.stripe.android.common.nfcscan.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.paymentsheet.ui.PrimaryButtonTheme

internal val CoilCircleSize = 160.dp
private val ShadowElevation = 8.dp

@Composable
internal fun NfcCoilLayout(
    status: NfcScanningStatus,
    tapZone: TapZone,
    deviceRotation: DeviceRotation,
    onSuccessShown: () -> Unit,
    onErrorShown: () -> Unit,
) {
    val coilScaleState = rememberNfcCoilScaleState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = BiasAlignment(
                horizontalBias = tapZone.xBias * 2 - 1,
                verticalBias = tapZone.yBias * 2 - 1,
            ),
        ) {
            StatefulPulseRings(
                status = status,
                coilScaleState = coilScaleState,
            )

            StatefulNfcCoil(
                status = status,
                coilScaleState = coilScaleState,
                onSuccessShown = onSuccessShown,
                modifier = Modifier.graphicsLayer {
                    val coilScale = coilScaleState.value.coilScale

                    scaleX = coilScale
                    scaleY = coilScale
                },
            )
        }

        StatefulNfcCoilTextLayout(
            status = status,
            tapZone = tapZone,
            nfcCoilScaleState = coilScaleState,
            deviceRotation = deviceRotation,
            onErrorShown = onErrorShown,
        )
    }
}

@Composable
private fun StatefulNfcCoil(
    status: NfcScanningStatus,
    coilScaleState: State<NfcCoilScaleState>,
    onSuccessShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()

    val shadow = if (isDarkTheme) {
        Modifier
    } else {
        Modifier.shadow(elevation = ShadowElevation, shape = CircleShape)
    }

    val coilState by coilScaleState
    val alpha = animateFloatAsState(
        targetValue = if (coilState.isComplete) {
            MAX_PROGRESS
        } else {
            MIN_PROGRESS
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    Box(
        modifier = modifier
            .size(CoilCircleSize)
            .then(shadow)
            .background(color = PrimaryButtonTheme.colors.background, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha.value
            }
        ) {
            NfcCoilAnimatedInterior(
                status = status,
                onSuccessShown = onSuccessShown,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.StatefulNfcCoilTextLayout(
    status: NfcScanningStatus,
    deviceRotation: DeviceRotation,
    tapZone: TapZone,
    nfcCoilScaleState: State<NfcCoilScaleState>,
    onErrorShown: () -> Unit,
) {
    val shouldRenderTextAboveCoil = rememberOrientationValues(
        deviceRotation = deviceRotation,
        onPortrait = { tapZone.yBias > 0.75 },
        onLandscape = { tapZone.yBias > 0.6 },
    )

    val idleStatus = status as? NfcScanningStatus.Idle
    val coilState by nfcCoilScaleState

    NfcCoilTextLayout(
        containerWidth = maxWidth,
        containerHeight = maxHeight,
        tapZone = tapZone,
        shouldRenderTextAboveCoil = shouldRenderTextAboveCoil,
        coilSize = CoilCircleSize,
        deviceRotation = deviceRotation,
        canShow = status !is NfcScanningStatus.Scanned && coilState.isComplete,
        error = idleStatus?.message?.let { message ->
            ErrorBannerParams(
                message = message,
                onShown = onErrorShown,
            )
        },
    )
}

@Composable
private fun StatefulPulseRings(
    status: NfcScanningStatus,
    coilScaleState: State<NfcCoilScaleState>
) {
    val coilState by coilScaleState

    if (status is NfcScanningStatus.Idle && coilState.isComplete) {
        PulseRings(coilSize = CoilCircleSize)
    }
}

private const val MIN_PROGRESS = 0f
private const val MAX_PROGRESS = 1f
