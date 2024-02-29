package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.H6Text

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_DIALOG_CONFIRM_BUTTON = "simple_dialog_confirm_button"

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
                        },
                        modifier = Modifier
                            .testTag(TEST_TAG_DIALOG_CONFIRM_BUTTON),
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
