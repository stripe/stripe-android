package com.stripe.android.paymentsheet.example.playground

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MerchantOverrideDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var publicKeyField by remember { mutableStateOf("") }
    var privateKeyField by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = "Enter merchant details",
                style = MaterialTheme.typography.h6
            )
        },
        text = {
            Column {
                TextField(
                    value = publicKeyField,
                    onValueChange = { publicKeyField = it },
                    label = { Text("Public key") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = privateKeyField,
                    onValueChange = { privateKeyField = it },
                    label = { Text("Private key") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(publicKeyField, privateKeyField) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(
                onClick = { onDismiss() }
            ) {
                Text("Cancel")
            }
        }
    )
}
