package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun H4Text(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.h4,
        modifier = modifier
    )
}
