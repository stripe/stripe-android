package com.stripe.android.uicore.elements

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.stripeColorScheme

@Composable
internal fun FormLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.stripeColorScheme.placeholderText,
        style = MaterialTheme.typography.titleMedium
    )
}
