package com.stripe.android.crypto.onramp.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

@Composable
internal fun OnrampSpinner(
    modifier: Modifier,
    filledColor: Color,
    backgroundColor: Color,
    strokeWidth: Dp
) {
    val transition = rememberInfiniteTransition()
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidth.toPx()
        drawCircle(
            color = backgroundColor,
            radius = size.minDimension / 2 - strokeWidthPx / 2,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round
            )
        )
        drawArc(
            color = filledColor,
            startAngle = angle,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round
            ),
            size = Size(
                width = size.width - strokeWidthPx,
                height = size.height - strokeWidthPx
            ),
            topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
        )
    }
}
