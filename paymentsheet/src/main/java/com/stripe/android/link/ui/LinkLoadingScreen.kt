package com.stripe.android.link.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme

/**
 * Displays a loading spinner in the center of the screen.
 * Will match the last screen size as provided in [LocalLinkScreenSize] to minimize size changes.
 */
@Composable
internal fun LinkLoadingScreen(
    modifier: Modifier = Modifier,
    minHeight: Dp? = LocalLinkScreenSize.current?.height,
) {
    Box(
        modifier = modifier
            .then(minHeight?.let { Modifier.heightIn(min = it) } ?: Modifier)
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        LinkSpinner(modifier = Modifier.size(48.dp))
    }
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
        LinkLoadingScreen(minHeight = 500.dp)
    }
}
