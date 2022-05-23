package com.stripe.android.ui.core.elements

import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.stripe.android.ui.core.paymentsColors

@Composable
internal fun FormLabel(
    text: String,
    enabled: Boolean = true
) {
    val color = MaterialTheme.paymentsColors.placeholderText
    Text(
        color = if (enabled) color else color.copy(alpha = ContentAlpha.disabled),
        text = text,
        style = MaterialTheme.typography.subtitle1
    )
}
