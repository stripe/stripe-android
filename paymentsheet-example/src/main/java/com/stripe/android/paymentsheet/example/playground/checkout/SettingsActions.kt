package com.stripe.android.paymentsheet.example.playground.checkout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun SettingsActions(
    canStart: Boolean,
    onStart: () -> Unit,
    onReset: () -> Unit,
) {
    Button(
        onClick = onStart,
        enabled = canStart,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Create Checkout Session")
    }
    TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
        Text("Reset to defaults")
    }
}
