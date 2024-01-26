package com.stripe.android.uicore.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

internal object SailTheme {
    val colors: SailColors
        @Composable
        get() = LocalSailColors.current
}

internal val LocalSailColors = staticCompositionLocalOf<SailColors> {
    error("No SailColors provided")
}
