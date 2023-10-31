package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.H6Text

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SimpleDialogElementUI(
    openDialog: Boolean,
    titleText: String,
    messageText: String,
    confirmText: String,
    dismissText: String,
    onConfirmListener: (() -> Unit) = {},
    onDismissListener: (() -> Unit) = {}
) {
    if (openDialog) {
        StripeTheme {
            AlertDialog(
                onDismissRequest = {
                    onDismissListener()
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
                            onConfirmListener()
                        }
                    ) {
                        Text(confirmText)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
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
