package com.stripe.android.uicore.elements

import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.stripeColors

@Composable
internal fun FormLabel(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val color = MaterialTheme.stripeColors.placeholderText
    Text(
        text = text,
        modifier = modifier,
        color = if (enabled) color else color.copy(alpha = ContentAlpha.disabled),
        style = MaterialTheme.typography.subtitle1
    )
}
