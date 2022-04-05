package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.ui.core.PaymentsTheme

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun H6Text(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = PaymentsTheme.colors.subtitle,
        style = PaymentsTheme.typography.h6,
        modifier = modifier
    )
}
