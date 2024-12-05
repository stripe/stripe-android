package com.stripe.android.ui.core.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Stable
interface Spec {
    @Composable
    fun Content(modifier: Modifier)
}
