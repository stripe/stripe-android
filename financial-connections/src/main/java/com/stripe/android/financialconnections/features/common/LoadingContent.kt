package com.stripe.android.financialconnections.features.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarState
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.Neutral900
import kotlinx.coroutines.delay

private const val SHIMMER_SIZE_MULTIPLIER = 0.2f
private const val SHIMMER_GRADIENT_ALPHA = 0.4f

private const val LOADING_SPINNER_ROTATION_MS = 1_000
private const val ShowLoadingPillDelayMillis = 5_000L
private const val SlideDurationMillis = 600

@Composable
internal fun LoadingShimmerEffect(
    content: @Composable (Brush) -> Unit
) {
    val screenWidthDp = with(LocalConfiguration.current) { screenWidthDp.dp }
    val screenWidth = with(LocalDensity.current) { screenWidthDp.toPx() }
    val shimmerWidth = screenWidth * SHIMMER_SIZE_MULTIPLIER

    val gradient = listOf(
        colors.backgroundOffset,
        Color.White.copy(alpha = SHIMMER_GRADIENT_ALPHA),
        colors.backgroundOffset
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
        LoadingSpinner(Modifier.size(52.dp))
    }
}

@Composable
internal fun LoadingSpinner(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = ProgressIndicatorDefaults.StrokeWidth,
    gradient: Brush = Brush.sweepGradient(listOf(colors.backgroundSurface, colors.borderBrand)),
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

@Composable
internal fun LoadingPillContainer(
    canShowPill: Boolean,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val inspectionMode = LocalInspectionMode.current

    val bottomOfScreen = remember(configuration, density) {
        with(density) {
            configuration.screenHeightDp.dp.roundToPx()
        }
    }

    var showingPill by rememberSaveable {
        mutableStateOf(inspectionMode && canShowPill)
    }

    LaunchedEffect(canShowPill) {
        if (canShowPill) {
            delay(ShowLoadingPillDelayMillis)
        }
        showingPill = canShowPill
    }

    AnimatedVisibility(
        visible = showingPill,
        enter = slideInVertically(
            animationSpec = tween(
                durationMillis = SlideDurationMillis,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetY = { bottomOfScreen },
        ),
        exit = slideOutVertically(
            animationSpec = tween(SlideDurationMillis),
            targetOffsetY = { bottomOfScreen },
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            LoadingPill()
        }
    }
}

@Composable
private fun LoadingPill(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(
                color = Neutral900,
                shape = RoundedCornerShape(percent = 100),
            )
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            )
    ) {
        Text(
            text = stringResource(R.string.stripe_loading_pill_label),
            color = colors.textWhite,
            style = typography.bodySmall,
        )

        LoadingSpinner(
            strokeWidth = 2.dp,
            // TODO: Does this need to change for dark mode?
            gradient = Brush.sweepGradient(listOf(Color.Transparent, colors.iconWhite)),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Preview(
    group = "Loading",
    name = "Default"
)
@Composable
internal fun LoadingSpinnerPreview() {
    FinancialConnectionsPreview {
        FinancialConnectionsScaffold(
            topBar = {
                FinancialConnectionsTopAppBar(
                    state = TopAppBarState(hideStripeLogo = false),
                    onCloseClick = {},
                )
            },
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
            topBar = {
                FinancialConnectionsTopAppBar(
                    state = TopAppBarState(hideStripeLogo = false),
                    onCloseClick = {},
                )
            },
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

@Preview(
    group = "Loading",
    name = "Shimmer with Pill",
)
@Composable
internal fun LoadingShimmerWithPillPreview() {
    FinancialConnectionsPreview {
        FinancialConnectionsScaffold(
            topBar = {
                FinancialConnectionsTopAppBar(
                    state = TopAppBarState(hideStripeLogo = false),
                    onCloseClick = {},
                )
            },
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
                    Spacer(modifier = Modifier.weight(1f))
                    LoadingPillContainer(
                        canShowPill = true,
                    )
                }
            }
        )
    }
}
