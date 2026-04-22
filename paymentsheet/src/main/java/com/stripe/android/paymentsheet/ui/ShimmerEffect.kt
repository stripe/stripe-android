package com.stripe.android.paymentsheet.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private const val SHIMMER_DURATION_MS = 1_200
private const val SHIMMER_SIZE_MULTIPLIER = 0.2f
private const val SHIMMER_GRADIENT_ALPHA = 0.4f

@Composable
internal fun ShimmerEffect(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val screenWidthDp = with(LocalConfiguration.current) { screenWidthDp.dp }
    val screenWidthPx = with(LocalDensity.current) { screenWidthDp.toPx() }
    val shimmerWidth = screenWidthPx * SHIMMER_SIZE_MULTIPLIER

    val transition = rememberInfiniteTransition(label = "card_art_shimmer")
    val translateAnimation by transition.animateFloat(
        label = "card_art_shimmer_translate",
        initialValue = 0f,
        targetValue = screenWidthPx,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SHIMMER_DURATION_MS,
                easing = LinearEasing,
            ),
        ),
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.Gray.copy(alpha = 0f),
            Color.DarkGray.copy(alpha = SHIMMER_GRADIENT_ALPHA),
            Color.Gray.copy(alpha = 0f),
        ),
        start = Offset(translateAnimation - shimmerWidth, 0f),
        end = Offset(translateAnimation, 0f),
    )

    Box(modifier = modifier) {
        content()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shimmerBrush),
        )
    }
}
