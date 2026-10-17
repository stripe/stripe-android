package com.stripe.android.common.nfcscan.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.PrimaryButtonTheme

@Composable
internal fun Spinner(modifier: Modifier = Modifier) {
    val color = PrimaryButtonTheme.colors.onBackground

    val spinnerTransition = rememberInfiniteTransition(label = "spinner")
    val spinnerRotation by spinnerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(SPINNER_DURATION, easing = LinearEasing)),
        label = "spinnerRotation",
    )

    Box(modifier = modifier.rotate(spinnerRotation).testTag(SPINNER_TEST_TAG)) {
        Canvas(Modifier.size(36.dp).padding(2.dp)) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 300f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

private const val SPINNER_DURATION = 600

internal const val SPINNER_TEST_TAG = "nfc_spinner"
