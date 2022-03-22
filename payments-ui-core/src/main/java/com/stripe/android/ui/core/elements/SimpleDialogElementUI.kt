package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.stripe.android.ui.core.PaymentsTheme

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SimpleDialogElementUI(
    openDialog: MutableState<Boolean>,
    titleText: String,
    messageText: String,
    confirmText: String,
    dismissText: String,
    onConfirmListener: (() -> Unit) = {},
    onDismissListener: (() -> Unit) = {}
) {

    if (openDialog.value) {
        PaymentsTheme {
            AlertDialog(
                onDismissRequest = {
                    openDialog.value = false
                },
                title = {
                    H4Text(text = titleText)
                },
                text = {
                    H6Text(text = messageText)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            openDialog.value = false
                            onConfirmListener()
                        }
                    ) {
                        Text(confirmText)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            openDialog.value = false
                            onDismissListener()
                        }
                    ) {
                        Text(dismissText)
                    }
                }
            )
        }
    }
}
