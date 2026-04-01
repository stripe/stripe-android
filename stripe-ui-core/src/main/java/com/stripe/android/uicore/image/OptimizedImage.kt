package com.stripe.android.uicore.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun rememberOptimizedImage(
    url: String?,
    width: Dp
): String? {
    val density = LocalDensity.current
    val imageOptimizer = LocalImageOptimizer.current
    return url?.let {
        imageOptimizer.optimize(
            url = url,
            width = with(density) {
                width.roundToPx()
            }
        )
    }
}
