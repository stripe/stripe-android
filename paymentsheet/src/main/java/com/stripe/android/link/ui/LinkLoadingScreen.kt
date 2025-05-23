package com.stripe.android.link.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme

private val LocalLinkScreenSize = compositionLocalOf<DpSize?> { null }

/**
 * Displays a loading spinner in the center of the screen.
 * Will match the last screen size using [ProvideLinkScreenSize] to minimize size changes.
 */
@Composable
internal fun LinkLoadingScreen(modifier: Modifier = Modifier) {
    val screenSize = LocalLinkScreenSize.current
    Box(
        modifier = modifier
            .then(if (screenSize != null) Modifier.heightIn(min = screenSize.height) else Modifier)
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        LinkSpinner(modifier = Modifier.size(48.dp))
    }
}

@Composable
internal fun ProvideLinkScreenSize(size: IntSize?, content: @Composable () -> Unit) {
    val dpSize = with(LocalDensity.current) {
        size?.let { DpSize(it.width.toDp(), it.height.toDp()) }
    }
    CompositionLocalProvider(
        LocalLinkScreenSize provides dpSize,
        content = content
    )
}

@Preview
@Composable
private fun LinkLoadingScreenPreview() {
    DefaultLinkTheme {
        LinkLoadingScreen()
    }
}

@Preview
@Composable
private fun LinkLoadingScreenWithScreenSizePreview() {
    DefaultLinkTheme {
        CompositionLocalProvider(LocalLinkScreenSize provides DpSize(200.dp, 500.dp)) {
            LinkLoadingScreen()
        }
    }
}
