package com.stripe.android.link.theme

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object LinkTheme {

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
