package com.stripe.android.financialconnections.features.common

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun LoadingContent(
    title: String? = null,
    content: String? = null
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.size(24.dp))
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

private const val LOADING_SPINNER_ROTATION_MS = 1000

@Composable
@Preview()
internal fun LoadingSpinnerPreview() {
    LoadingSpinner()
}
