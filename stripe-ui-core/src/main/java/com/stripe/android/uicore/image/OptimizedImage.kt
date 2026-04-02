package com.stripe.android.uicore.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun rememberOptimizedImage(
    url: String?,
    width: Dp
): String? {
    if (url == null) return null
    val density = LocalDensity.current
    val imageOptimizer = LocalImageOptimizer.current
    return imageOptimizer.optimize(
        url = url,
        width = with(density) {
            width.roundToPx()
        }
    )
}
