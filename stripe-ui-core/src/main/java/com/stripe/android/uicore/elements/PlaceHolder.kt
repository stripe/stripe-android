package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.stripeColors

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun Placeholder(
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
