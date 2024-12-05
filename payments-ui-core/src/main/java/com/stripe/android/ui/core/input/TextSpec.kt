package com.stripe.android.ui.core.input

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

@Immutable
data class TextSpec(
    val text: String
) : Spec {
    @Composable
    override fun Content(modifier: Modifier) {
        Text(
            modifier = modifier,
            text = text
        )
    }
}
