package com.stripe.android.link.ui.wallet

import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stripe.android.link.R
import com.stripe.android.link.model.removeConfirmation
import com.stripe.android.link.theme.linkColors
import com.stripe.android.model.ConsumerPaymentDetails

@Composable
internal fun ConfirmRemoveDialog(
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    showDialog: Boolean,
    onDialogDismissed: (Boolean) -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                onDialogDismissed(false)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDialogDismissed(true)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.remove),
                        color = MaterialTheme.linkColors.actionLabel
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDialogDismissed(false)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = MaterialTheme.linkColors.actionLabel
                    )
                }
            },
            text = {
                Text(stringResource(paymentDetails.removeConfirmation))
            }
        )
    }
}
