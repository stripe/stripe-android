package com.stripe.android.paymentsheet.elements

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun MandateElementUI(
    element: MandateTextElement
) {
    Text(
        stringResource(element.stringResId, element.merchantName ?: ""),
        fontSize = 10.sp,
        letterSpacing = .7.sp,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {}, // makes it a separate accessibile item
        color = element.color
    )
}
