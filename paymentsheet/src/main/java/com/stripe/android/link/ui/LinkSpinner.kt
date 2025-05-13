package com.stripe.android.link.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme

@Composable
internal fun LinkSpinner(
    modifier: Modifier = Modifier,
    filledColor: Color = LinkTheme.colors.iconBrand,
    backgroundColor: Color = LinkTheme.colors.surfaceTertiary,
    strokeWidth: Dp = 6.dp
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

@Preview
@Composable
private fun LinkSpinnerPreview() {
    DefaultLinkTheme {
        LinkSpinner(
            modifier = Modifier.size(24.dp),
            strokeWidth = 6.dp
        )
    }
}
