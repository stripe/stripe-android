package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.PaymentsTheme

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun MandateTextUI(
    element: MandateTextElement
) {
    Text(
        text = stringResource(element.stringResId, element.merchantName ?: ""),
        style = PaymentsTheme.typography.body2,
        color = PaymentsTheme.colors.colorTextSecondary,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {}, // makes it a separate accessibile item
    )
}
