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
                ValidatedTextField(
                    label = "Public key",
                    value = publicKeyField,
                    validator = { if (it.startsWith("pk_")) null else "Public key must start with 'pk_'" },
                    onValueChange = { publicKeyField = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ValidatedTextField(
                    label = "Private key",
                    value = privateKeyField,
                    validator = { if (it.startsWith("sk_")) null else "Public key must start with 'sk_'" },
                    onValueChange = { privateKeyField = it }
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

@Composable
private fun ValidatedTextField(
    label: String,
    value: String,
    validator: (String) -> String?,
    onValueChange: (String) -> Unit
) {
    var errorMessage: String? by remember { mutableStateOf(null) }
    Column {
        TextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                errorMessage = validator(it)
            },
            label = { Text(label) },
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
