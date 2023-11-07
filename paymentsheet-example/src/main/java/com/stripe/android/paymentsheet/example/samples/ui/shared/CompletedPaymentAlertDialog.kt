package com.stripe.android.paymentsheet.example.samples.ui.shared

import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.example.R

@Composable
fun CompletedPaymentAlertDialog(
    title: String? = null,
    confirmButtonText: String? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                }
            ) {
                Text(
                    text = confirmButtonText ?: stringResource(
                        id = R.string.finish
                    )
                )
            }
        },
        modifier = Modifier.padding(horizontal = 32.dp),
        title = {
            Text(
                text = title ?: stringResource(
                    id = R.string.success
                )
            )
        }
    )
}
