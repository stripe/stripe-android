package com.stripe.android.lpmfoundations

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A Jetpack Compose renderer. The [Content] method will be called when it's ready to be added to the screen.
 */
internal fun interface UiRenderer {
    @Composable
    fun Content(enabled: Boolean, modifier: Modifier)
}
