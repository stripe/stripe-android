package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.stripeColorScheme

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun Placeholder(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val color = MaterialTheme.stripeColorScheme.placeholderText
    Text(
        text = text,
        modifier = modifier,
        color = if (enabled) color else color.copy(alpha = .38f),
        style = MaterialTheme.typography.titleMedium
    )
}
