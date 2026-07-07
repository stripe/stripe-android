package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val CHECKMARK_SIZE_FRACTION = 0.228f
private const val CHECKMARK_START_Y_OFFSET_FRACTION = 0.07f
private const val CHECKMARK_MIDDLE_X_OFFSET_FRACTION = 0.28f
private const val CHECKMARK_VERTICAL_OFFSET_FRACTION = 0.62f

private val ArcInset = 7.dp
private val ArcStrokeWidth = 3.dp

@Composable
internal fun NfcCoilSuccessRing(
    arcProgress: Float,
    checkmarkProgress: Float,
    checkmarkAlpha: Float,
    modifier: Modifier = Modifier,
) {
    if (arcProgress <= 0f && checkmarkProgress <= 0f) {
        return
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val strokeWidth = ArcStrokeWidth.toPx()

        if (arcProgress > 0f) {
            drawArc(
                center = center,
                radius = radius,
                progress = arcProgress,
                strokeWidth = strokeWidth,
            )
        }
        if (checkmarkProgress > 0f) {
            drawCheckmark(
                center = center,
                radius = radius,
                progress = checkmarkProgress,
                alpha = checkmarkAlpha,
                strokeWidth = strokeWidth,
            )
        }
    }
}

private fun DrawScope.drawArc(
    center: Offset,
    radius: Float,
    progress: Float,
    strokeWidth: Float,
) {
    val arcRadius = radius - ArcInset.toPx()
    drawArc(
        color = Color.White,
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
        size = Size(arcRadius * 2f, arcRadius * 2f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
    )
}

private fun DrawScope.drawCheckmark(
    center: Offset,
    radius: Float,
    progress: Float,
    alpha: Float,
    strokeWidth: Float,
) {
    val checkmarkRadius = radius * CHECKMARK_SIZE_FRACTION

    val path = Path().apply {
        moveTo(
            center.x - checkmarkRadius,
            center.y - checkmarkRadius * CHECKMARK_START_Y_OFFSET_FRACTION,
        )
        lineTo(
            center.x - checkmarkRadius * CHECKMARK_MIDDLE_X_OFFSET_FRACTION,
            center.y + checkmarkRadius * CHECKMARK_VERTICAL_OFFSET_FRACTION,
        )
        lineTo(
            center.x + checkmarkRadius,
            center.y - checkmarkRadius * CHECKMARK_VERTICAL_OFFSET_FRACTION,
        )
    }

    val pathMeasure = PathMeasure().apply { setPath(path, forceClosed = false) }
    val drawnPath = Path()

    pathMeasure.getSegment(
        startDistance = 0f,
        stopDistance = pathMeasure.length * progress,
        destination = drawnPath,
        startWithMoveTo = true,
    )

    drawPath(
        path = drawnPath,
        color = Color.White.copy(alpha = alpha),
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Butt,
            join = StrokeJoin.Miter,
        ),
    )
}
