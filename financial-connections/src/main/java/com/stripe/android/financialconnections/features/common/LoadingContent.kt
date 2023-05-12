package com.stripe.android.financialconnections.features.common

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

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
    val transition = rememberInfiniteTransition()
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
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
        LoadingSpinner()
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
internal fun LoadingSpinner() {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(LOADING_SPINNER_ROTATION_MS)
        )
    )
    Image(
        painter = painterResource(id = R.drawable.stripe_ic_loading_spinner),
        modifier = Modifier.graphicsLayer { rotationZ = angle },
        contentDescription = "Loading spinner."
    )
}

@Composable
internal fun FullScreenGenericLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = FinancialConnectionsTheme.colors.textSecondary,
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
    LoadingSpinner()
}
