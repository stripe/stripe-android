package com.stripe.android.financialconnections.features.common

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors

@Composable
@Suppress("MagicNumber")
internal fun LoadingShimmerEffect(
    content: @Composable (Brush) -> Unit
) {
    val gradient = listOf(
        FinancialConnectionsTheme.colors.backgroundContainer,
        FinancialConnectionsTheme.colors.textWhite,
        FinancialConnectionsTheme.colors.backgroundContainer
    )
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnimation = transition.animateFloat(
        label = "shimmer_translate_animation",
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = LOADING_SPINNER_ROTATION_MS,
                easing = FastOutLinearInEasing
            )
        )
    )
    val brush = Brush.linearGradient(
        colors = gradient,
        start = Offset(200f, 200f),
        end = Offset(
            x = translateAnimation.value,
            y = translateAnimation.value
        )
    )
    content(brush)
}

@Composable
internal fun LoadingContent(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: String? = null
) {
    Column(
        modifier = modifier
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.size(8.dp))
        LoadingSpinner(modifier = Modifier.size(32.dp))
        if (title != null) {
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = title,
                style = FinancialConnectionsTheme.typography.subtitle
            )
        }
        if (content != null) {
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = content,
                style = FinancialConnectionsTheme.typography.body
            )
        }
    }
}

@Composable
internal fun FullScreenGenericLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingSpinner(
            modifier = Modifier.size(56.dp),
        )
    }
}

@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = ProgressIndicatorDefaults.StrokeWidth,
    gradient: Brush = Brush.sweepGradient(listOf(v3Colors.iconWhite, v3Colors.borderBrand))
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_transition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(LOADING_SPINNER_ROTATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_animation"
    )

    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val radius = diameter / 2f
        val strokePx = strokeWidth.toPx()
        val arcDiameter = diameter - strokePx
        val arcRadius = arcDiameter / 2f
        val topLeftArc = Offset(radius - arcRadius, radius - arcRadius)

        withTransform(
            transformBlock = {
                rotate(
                    degrees = angle,
                    pivot = Offset(size.width / 2, size.height / 2)
                )
            },
            drawBlock = {
                drawArc(
                    brush = gradient,
                    startAngle = 90f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = topLeftArc,
                    size = Size(arcDiameter, arcDiameter),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        )
    }
}

private const val LOADING_SPINNER_ROTATION_MS = 1000

@Preview(
    group = "Loading",
    name = "Default"
)
@Composable
internal fun LoadingSpinnerPreview() {
    FinancialConnectionsPreview {
        FullScreenGenericLoading()
    }
}
