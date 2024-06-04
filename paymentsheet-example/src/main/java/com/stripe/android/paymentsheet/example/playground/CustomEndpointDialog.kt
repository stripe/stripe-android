package com.stripe.android.paymentsheet.example.playground

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CustomEndpointDialog(
    currentUrl: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var urlField by remember { mutableStateOf(currentUrl ?: "") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = "Enter custom endpoint URL",
                style = MaterialTheme.typography.h6
            )
        },
        text = {
            Column {
                ValidatedTextField(
                    label = "Backend url",
                    value = urlField,
                    validator = {
                        when {
                            it.startsWith("http").not() -> "Must be a valid URL"
                            it.endsWith("/").not() -> "Must end with /"
                            else -> null
                        }
                    },
                    onValueChange = { urlField = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(urlField) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onConfirm(null) }
            ) {
                Text("Reset")
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
