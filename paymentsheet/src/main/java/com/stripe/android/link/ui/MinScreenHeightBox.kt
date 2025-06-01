package com.stripe.android.link.ui

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * A [Box] that enforces a minimum height relative to the screen height.
 */
@Composable
internal fun MinScreenHeightBox(
    @FloatRange(0.0, 1.0) screenHeightPercentage: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    BoxWithConstraints {
        val minScreenHeight = (screenHeight * screenHeightPercentage).dp.coerceAtMost(maxHeight)
        Box(
            modifier = Modifier.heightIn(min = minScreenHeight),
            content = content
        )
    }
}
