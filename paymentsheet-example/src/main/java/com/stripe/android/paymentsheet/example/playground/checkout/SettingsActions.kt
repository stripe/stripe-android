package com.stripe.android.paymentsheet.example.playground.checkout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
internal fun SettingsActions(
    canStart: Boolean,
    onStart: () -> Unit,
) {
    Button(
        onClick = onStart,
        enabled = canStart,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Create Checkout Session")
    }
}

@Composable
internal fun SettingsOverflowMenu(
    onRunScenario: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onReset: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(
        onClick = { expanded = true },
        modifier = Modifier.semantics { contentDescription = "More options" },
    ) {
        Text("⋮", style = MaterialTheme.typography.h5)
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        SettingsDropdownMenuItem(
            text = "Run scenario",
            onClick = onRunScenario,
            onDismiss = { expanded = false },
        )
        SettingsDropdownMenuItem(
            text = "Import settings from JSON",
            onClick = onImport,
            onDismiss = { expanded = false },
        )
        SettingsDropdownMenuItem(
            text = "Export settings as JSON",
            onClick = onExport,
            onDismiss = { expanded = false },
        )
        SettingsDropdownMenuItem(
            text = "Reset to defaults",
            onClick = onReset,
            onDismiss = { expanded = false },
        )
    }
}

@Composable
private fun SettingsDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenuItem(
        onClick = {
            onDismiss()
            onClick()
        },
    ) {
        Text(text)
    }
}
