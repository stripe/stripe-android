package com.stripe.android.common.nfcscan.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Three rings expanding 0.8→2.5 and fading, staggered by 1/3 of a 4.5s cycle. */
@Composable
internal fun PulseRings(coilSize: Dp) {
    val transition = rememberInfiniteTransition(label = "pulsing_rings")

    val transitionProgress = if (LocalInspectionMode.current) {
        STATIC_PROGRESS
    } else {
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(CYCLE_DURATION, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "pulsing_rings_animation",
        )

        progress
    }

    val color = MaterialTheme.colors.primary

    RING_INDICES.forEach { index ->
        val progress = (transitionProgress + index / NUM_RINGS) % 1f
        val scale = MIN_SIZE_SCALE + progress * (MAX_SIZE_SCALE - MIN_SIZE_SCALE)
        val alpha = (1f - progress) * MAX_RING_ALPHA

        Box(
            modifier = Modifier
                .size(coilSize)
                .scale(scale)
                .alpha(alpha)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.04f)),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = color.copy(alpha = 0.5f),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
    }
}

private const val CYCLE_DURATION = 4500

private val RING_INDICES = listOf(0, 1, 2)
private val NUM_RINGS = RING_INDICES.size.toFloat()

private const val MAX_RING_ALPHA = 0.8f

private const val MIN_SIZE_SCALE = 0.8f
private const val MAX_SIZE_SCALE = 2.5f

private const val STATIC_PROGRESS = 0.75f
