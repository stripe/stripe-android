package com.stripe.android.link.theme

import androidx.compose.runtime.Composable

internal object LinkTheme {

    val typography: LinkTypography
        @Composable
        get() = LocalLinkTypography.current

    val colors: LinkColors
        @Composable
        get() = LocalLinkColors.current

    val shapes: LinkShapes
        @Composable
        get() = LocalLinkShapes.current
}
