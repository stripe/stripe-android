package com.stripe.android.ui.core.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SpecBox(
    modifier: Modifier = Modifier,
    spec: Spec
) {
    spec.Content(modifier)
}
