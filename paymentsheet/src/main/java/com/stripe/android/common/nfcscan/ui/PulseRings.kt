package com.stripe.android.common.nfcscan.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Three rings expanding 0.8→2.5 and fading, staggered by 1/3 of a 4.5s cycle. */
@Composable
internal fun PulseRings(coilSize: Dp) {
    val color = MaterialTheme.colors.primary

    if (LocalInspectionMode.current) {
        PulseRingsCanvas(
            coilSize = coilSize,
            color = color,
            transitionProgress = STATIC_PROGRESS,
        )
    } else {
        val transition = rememberInfiniteTransition(label = "pulsing_rings")
        val transitionProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(CYCLE_DURATION, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "pulsing_rings_animation",
        )

        PulseRingsCanvas(
            coilSize = coilSize,
            color = color,
            transitionProgress = transitionProgress,
        )
    }
}

@Composable
private fun PulseRingsCanvas(
    coilSize: Dp,
    color: Color,
    transitionProgress: Float,
) {
    Canvas(modifier = Modifier.size(coilSize)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension / 2f
        val strokeWidth = 1.dp.toPx()

        repeat(RING_COUNT) { index ->
            val progress = (transitionProgress + index / NUM_RINGS) % 1f
            val scale = MIN_SIZE_SCALE + progress * (MAX_SIZE_SCALE - MIN_SIZE_SCALE)
            val alpha = (1f - progress) * MAX_RING_ALPHA
            val radius = baseRadius * scale

            drawCircle(
                color = color.copy(alpha = RING_FILL_ALPHA * alpha),
                radius = radius,
                center = center,
            )
            drawCircle(
                color = color.copy(alpha = RING_STROKE_ALPHA * alpha),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

private const val CYCLE_DURATION = 4500
private const val RING_COUNT = 3
private const val NUM_RINGS = RING_COUNT.toFloat()

private const val MAX_RING_ALPHA = 0.8f
private const val RING_FILL_ALPHA = 0.04f
private const val RING_STROKE_ALPHA = 0.5f

private const val MIN_SIZE_SCALE = 0.8f
private const val MAX_SIZE_SCALE = 2.5f

private const val STATIC_PROGRESS = 0.75f
