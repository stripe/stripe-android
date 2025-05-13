package com.stripe.android.link.ui

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme

@Composable
internal fun LinkScreenshotSurface(
    content: @Composable () -> Unit
) {
    DefaultLinkTheme {
        Surface(
            color = LinkTheme.colors.surfacePrimary,
            content = content
        )
    }
}
