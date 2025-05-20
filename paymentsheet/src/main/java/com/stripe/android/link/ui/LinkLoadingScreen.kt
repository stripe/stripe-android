package com.stripe.android.link.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Displays a loading spinner in the center of the screen.
 * Tries to match the last screen size using [LocalLinkScreenSize] to minimize size changes.
 */
@Composable
internal fun LinkLoadingScreen(modifier: Modifier = Modifier) {
    val screenSize = LocalLinkScreenSize.current
    val screenSizeDp = with(LocalDensity.current) {
        screenSize?.let { DpSize(it.width.toDp(), it.height.toDp()) }
    }
    Box(
        modifier = modifier
            .then(if (screenSizeDp != null) Modifier.heightIn(min = screenSizeDp.height) else Modifier)
            .fillMaxWidth()
            .padding(64.dp),
        contentAlignment = Alignment.Center
    ) {
        LinkSpinner(modifier = Modifier.size(48.dp))
    }
}

internal val LocalLinkScreenSize = compositionLocalOf<IntSize?> { null }
