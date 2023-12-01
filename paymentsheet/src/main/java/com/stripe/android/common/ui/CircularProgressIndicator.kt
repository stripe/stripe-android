package com.stripe.android.common.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.material.CircularProgressIndicator as MaterialCircularProgressIndicator

@Composable
internal fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    strokeWidth: Dp = ProgressIndicatorDefaults.StrokeWidth,
    backgroundColor: Color = Color.Transparent,
    strokeCap: StrokeCap = StrokeCap.Butt,
) {
    if (LocalInspectionMode.current) {
        MaterialCircularProgressIndicator(
            progress = TEST_PROGRESS,
            color = color,
            strokeWidth = strokeWidth,
            backgroundColor = backgroundColor,
            strokeCap = strokeCap,
            modifier = modifier
        )
    } else {
        MaterialCircularProgressIndicator(
            color = color,
            strokeWidth = strokeWidth,
            backgroundColor = backgroundColor,
            strokeCap = strokeCap,
            modifier = modifier
        )
    }
}

private const val TEST_PROGRESS = 0.6f
