package com.stripe.android.ui.core.elements

import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.stripe.android.ui.core.PaymentsTheme

@Composable
internal fun FormLabel(
    text: String,
    enabled: Boolean = true
) {
    val color = PaymentsTheme.colors.placeholderText
    Text(
        color = if (enabled) color else color.copy(alpha = ContentAlpha.disabled),
        text = text,
        style = PaymentsTheme.typography.subtitle1
    )
}
