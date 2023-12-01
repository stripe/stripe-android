package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.H6Text

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SimpleDialogElementUI(
    titleText: String,
    messageText: String?,
    confirmText: String,
    dismissText: String,
    destructive: Boolean = false,
    onConfirmListener: () -> Unit,
    onDismissListener: () -> Unit,
) {
    StripeTheme {
        AlertDialog(
            onDismissRequest = {
                onDismissListener()
            },
            title = {
                H4Text(text = titleText)
            },
            text = messageText?.let {
                {
                    H6Text(text = it)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmListener()
                    }
                ) {
                    Text(
                        text = confirmText,
                        color = if (destructive) {
                            MaterialTheme.colors.error
                        } else {
                            Color.Unspecified
                        }
                    )
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
