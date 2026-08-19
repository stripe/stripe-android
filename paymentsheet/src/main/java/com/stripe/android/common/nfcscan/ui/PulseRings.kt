package com.stripe.android.common.nfcscan.ui

import android.os.SystemClock
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
            elapsedMillis = STATIC_PROGRESS * CYCLE_DURATION,
        )
    } else {
        val startMillis = rememberSaveable { SystemClock.elapsedRealtime() }
        var elapsedMillis by remember {
            mutableFloatStateOf((SystemClock.elapsedRealtime() - startMillis).toFloat())
        }

        LaunchedEffect(startMillis) {
            while (true) {
                withInfiniteAnimationFrameMillis {
                    elapsedMillis = (SystemClock.elapsedRealtime() - startMillis).toFloat()
                }
            }
        }

        PulseRingsCanvas(
            coilSize = coilSize,
            color = color,
            elapsedMillis = elapsedMillis,
        )
    }
}

@Composable
private fun PulseRingsCanvas(
    coilSize: Dp,
    color: Color,
    elapsedMillis: Float,
) {
    Canvas(modifier = Modifier.size(coilSize)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension / 2f
        val strokeWidth = 1.dp.toPx()

        repeat(RING_COUNT) { index ->
            val ringStartMillis = index / NUM_RINGS * CYCLE_DURATION
            val ringElapsedMillis = elapsedMillis - ringStartMillis

            if (ringElapsedMillis < 0f) {
                return@repeat
            }

            val progress = (ringElapsedMillis % CYCLE_DURATION) / CYCLE_DURATION
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

private const val CYCLE_DURATION = 4500f
private const val RING_COUNT = 3
private const val NUM_RINGS = RING_COUNT.toFloat()

private const val MAX_RING_ALPHA = 0.8f
private const val RING_FILL_ALPHA = 0.04f
private const val RING_STROKE_ALPHA = 0.5f

private const val MIN_SIZE_SCALE = 0.8f
private const val MAX_SIZE_SCALE = 2.5f

private const val STATIC_PROGRESS = 0.75f
