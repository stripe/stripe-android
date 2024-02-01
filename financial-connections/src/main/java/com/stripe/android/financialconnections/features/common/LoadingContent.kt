package com.stripe.android.financialconnections.features.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors

private const val SHIMMER_SIZE_MULTIPLIER = 0.2f
private const val SHIMMER_GRADIENT_ALPHA = 0.4f

@Composable
internal fun LoadingShimmerEffect(
    content: @Composable (Brush) -> Unit
) {
    val screenWidthDp = with(LocalConfiguration.current) { screenWidthDp.dp }
    val screenWidth = with(LocalDensity.current) { screenWidthDp.toPx() }
    val shimmerWidth = screenWidth * SHIMMER_SIZE_MULTIPLIER

    val gradient = listOf(
        v3Colors.backgroundOffset,
        Color.White.copy(alpha = SHIMMER_GRADIENT_ALPHA),
        v3Colors.backgroundOffset
    )
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnimation = transition.animateFloat(
        label = "shimmer_translate_animation",
        initialValue = 0f,
        targetValue = screenWidth,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = LOADING_SPINNER_ROTATION_MS,
                easing = LinearEasing
            )
        )
    )
    val brush = Brush.linearGradient(
        colors = gradient,
        start = Offset(
            translateAnimation.value - shimmerWidth,
            translateAnimation.value - shimmerWidth
        ),
        end = Offset(
            translateAnimation.value,
            translateAnimation.value
        )
    )
    content(brush)
}

@Composable
internal fun FullScreenGenericLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        V3LoadingSpinner(Modifier.size(52.dp))
    }
}

@Composable
internal fun V3LoadingSpinner(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = ProgressIndicatorDefaults.StrokeWidth,
    gradient: Brush = Brush.sweepGradient(listOf(v3Colors.iconWhite, v3Colors.borderBrand))
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_transition")
    val angle by infiniteTransition.animateFloat(
        label = "loading_animation",
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(LOADING_SPINNER_ROTATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
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
        FinancialConnectionsScaffold(
            topBar = { FinancialConnectionsTopAppBar(onCloseClick = {}) },
            content = {
                FullScreenGenericLoading()
            }
        )
    }
}

@Preview(
    group = "Loading",
    name = "Shimmer",

)
@Composable
internal fun LoadingShimmerPreview() {
    FinancialConnectionsPreview {
        FinancialConnectionsScaffold(
            topBar = { FinancialConnectionsTopAppBar(onCloseClick = {}) },
            content = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LoadingShimmerEffect {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(it)
                        )
                    }
                    LoadingShimmerEffect {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(it)
                        )
                    }
                }
            }
        )
    }
}
