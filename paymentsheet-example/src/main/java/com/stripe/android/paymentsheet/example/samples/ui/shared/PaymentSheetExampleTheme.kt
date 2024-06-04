package com.stripe.android.paymentsheet.example.samples.ui.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

@Composable
fun PaymentSheetExampleTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colors = if (isSystemInDarkTheme()) darkColors() else lightColors(),
        content = content,
    )
}
