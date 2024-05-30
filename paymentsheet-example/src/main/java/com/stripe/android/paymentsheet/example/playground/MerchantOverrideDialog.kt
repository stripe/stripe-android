package com.stripe.android.paymentsheet.example.playground

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    keys: Pair<String, String>?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var publicKeyField by remember { mutableStateOf(keys?.first ?: "") }
    var privateKeyField by remember { mutableStateOf(keys?.second ?: "") }
    var publicKeyError by remember { mutableStateOf(false) }
    var privateKeyError by remember { mutableStateOf(false) }
    val isConfirmEnabled = !publicKeyError && !privateKeyError

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
                    onValueChange = {
                        publicKeyField = it
                        publicKeyError = !it.startsWith("pk_")
                    },
                    isError = publicKeyError,
                    label = { Text("Public key") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (publicKeyError) {
                    Text(
                        text = "Public key must start with 'pk_'",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = privateKeyField,
                    isError = privateKeyError,
                    onValueChange = {
                        privateKeyField = it
                        privateKeyError = !it.startsWith("sk_")
                    },
                    label = { Text("Private key") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (privateKeyError) {
                    Text(
                        text = "Private key must start with 'sk_'",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = isConfirmEnabled,
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
