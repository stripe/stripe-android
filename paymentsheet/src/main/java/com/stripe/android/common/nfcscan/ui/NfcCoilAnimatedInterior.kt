package com.stripe.android.common.nfcscan.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R

private const val IDLE_ANIMATION_CYCLE_MS = 1_470
private const val IDLE_BAR_RISE_MS = 112
private const val IDLE_BAR_INTER_HOLD_MS = 70
private const val IDLE_ALL_BARS_HOLD_MS = 350
private const val IDLE_DIM_DURATION_MS = 140
private const val IDLE_BAR_DIM_ALPHA = 0.2f
private const val IDLE_BAR_BRIGHT_ALPHA = 1f
private const val IDLE_BAR1_RISE_END_MS = IDLE_BAR_RISE_MS
private const val IDLE_BAR2_RISE_START_MS = IDLE_BAR1_RISE_END_MS + IDLE_BAR_INTER_HOLD_MS
private const val IDLE_BAR2_RISE_END_MS = IDLE_BAR2_RISE_START_MS + IDLE_BAR_RISE_MS
private const val IDLE_BAR3_RISE_START_MS = IDLE_BAR2_RISE_END_MS + IDLE_BAR_INTER_HOLD_MS
private const val IDLE_BAR3_RISE_END_MS = IDLE_BAR3_RISE_START_MS + IDLE_BAR_RISE_MS
private const val IDLE_ALL_BARS_HOLD_END_MS = IDLE_BAR3_RISE_END_MS + IDLE_ALL_BARS_HOLD_MS
private const val IDLE_DIM_END_MS = IDLE_ALL_BARS_HOLD_END_MS + IDLE_DIM_DURATION_MS

private val CoilIconSize = 96.dp

@Composable
internal fun NfcCoilAnimatedInterior(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        NfcCoilSignalIcon(
            modifier = Modifier.size(CoilIconSize),
        )
    }
}

@Composable
private fun NfcCoilSignalIcon(
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        NfcCoilIcon(modifier = modifier)
        return
    }

    val barAlphas = rememberIdleBarAlphas()

    NfcCoilIcon(
        modifier = modifier,
        bar1Alpha = barAlphas.bar1,
        bar2Alpha = barAlphas.bar2,
        bar3Alpha = barAlphas.bar3,
    )
}

@Composable
private fun rememberIdleBarAlphas(): IdleBarAlphas {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_coil_idle")

    val bar1Alpha by infiniteTransition.animateFloat(
        initialValue = IDLE_BAR_DIM_ALPHA,
        targetValue = IDLE_BAR_DIM_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = idleBarAlphaKeyframes(
                riseStartMs = 0,
                riseEndMs = IDLE_BAR1_RISE_END_MS,
            ),
        ),
        label = "nfc_coil_idle_bar1",
    )
    val bar2Alpha by infiniteTransition.animateFloat(
        initialValue = IDLE_BAR_DIM_ALPHA,
        targetValue = IDLE_BAR_DIM_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = idleBarAlphaKeyframes(
                riseStartMs = IDLE_BAR2_RISE_START_MS,
                riseEndMs = IDLE_BAR2_RISE_END_MS,
            ),
        ),
        label = "nfc_coil_idle_bar2",
    )
    val bar3Alpha by infiniteTransition.animateFloat(
        initialValue = IDLE_BAR_DIM_ALPHA,
        targetValue = IDLE_BAR_DIM_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = idleBarAlphaKeyframes(
                riseStartMs = IDLE_BAR3_RISE_START_MS,
                riseEndMs = IDLE_BAR3_RISE_END_MS,
            ),
        ),
        label = "nfc_coil_idle_bar3",
    )

    return IdleBarAlphas(
        bar1 = bar1Alpha,
        bar2 = bar2Alpha,
        bar3 = bar3Alpha,
    )
}

private fun idleBarAlphaKeyframes(
    riseStartMs: Int,
    riseEndMs: Int,
) = keyframes {
    durationMillis = IDLE_ANIMATION_CYCLE_MS
    IDLE_BAR_DIM_ALPHA at 0 using LinearEasing
    if (riseStartMs > 0) {
        IDLE_BAR_DIM_ALPHA at riseStartMs using LinearEasing
    }
    IDLE_BAR_BRIGHT_ALPHA at riseEndMs using LinearEasing
    IDLE_BAR_BRIGHT_ALPHA at IDLE_ALL_BARS_HOLD_END_MS using LinearEasing
    IDLE_BAR_DIM_ALPHA at IDLE_DIM_END_MS using LinearEasing
    IDLE_BAR_DIM_ALPHA at IDLE_ANIMATION_CYCLE_MS
}

private data class IdleBarAlphas(
    val bar1: Float,
    val bar2: Float,
    val bar3: Float,
)

@Composable
internal fun NfcCoilIcon(
    modifier: Modifier = Modifier,
    bar1Alpha: Float = 1f,
    bar2Alpha: Float = 1f,
    bar3Alpha: Float = 1f,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.stripe_ic_material_nfc_coil_circle),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(R.drawable.stripe_ic_material_nfc_coil_bar1),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = bar1Alpha },
        )
        Image(
            painter = painterResource(R.drawable.stripe_ic_material_nfc_coil_bar2),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = bar2Alpha },
        )
        Image(
            painter = painterResource(R.drawable.stripe_ic_material_nfc_coil_bar3),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = bar3Alpha },
        )
    }
}
