package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.ui.core.paymentsColors

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun H6Text(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = MaterialTheme.paymentsColors.subtitle,
        style = MaterialTheme.typography.h6,
        modifier = modifier
    )
}
