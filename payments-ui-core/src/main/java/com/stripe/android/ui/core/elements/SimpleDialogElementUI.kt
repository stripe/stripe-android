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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_DIALOG_DISMISS_BUTTON = "simple_dialog_dismiss_button"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_SIMPLE_DIALOG = "simple_dialog"

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
            modifier = Modifier.testTag(TEST_TAG_SIMPLE_DIALOG),
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
                    modifier = Modifier.testTag(TEST_TAG_DIALOG_CONFIRM_BUTTON),
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
                        style = MaterialTheme.typography.body1
                    )
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag(TEST_TAG_DIALOG_DISMISS_BUTTON),
                    onClick = {
                        onDismissListener()
                    }
                ) {
                    Text(
                        text = dismissText,
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        )
    }
}
