package com.stripe.android.paymentsheet.elements.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
internal fun Mandate(text: String) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth(),
        color = Color.Gray
    )
}