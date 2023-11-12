package com.stripe.android.paymentsheet.prototype

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal fun interface UiRenderer {
    @Composable
    fun Content(enabled: Boolean, modifier: Modifier)
}
