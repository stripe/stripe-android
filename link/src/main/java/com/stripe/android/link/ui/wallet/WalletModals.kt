package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.R
import com.stripe.android.link.theme.HorizontalPadding

@Preview
@Composable
internal fun WalletBottomSheetContent() {
    WalletBottomSheetContent(
        onCancelClick = {},
        onEditClick = {},
        onRemoveClick = {}
    )
}

@Composable
internal fun WalletBottomSheetContent(
    onCancelClick: () -> Unit,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditClick)
        ) {
            Text(
                text = stringResource(R.string.wallet_update_card),
                modifier = Modifier
                    .padding(
                        start = HorizontalPadding,
                        top = 24.dp,
                        end = HorizontalPadding,
                        bottom = 10.dp
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onRemoveClick)
        ) {
            Text(
                text = stringResource(R.string.wallet_remove_card),
                modifier = Modifier
                    .padding(
                        horizontal = HorizontalPadding,
                        vertical = 10.dp
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCancelClick)
        ) {
            Text(
                text = stringResource(R.string.cancel),
                modifier = Modifier
                    .padding(
                        start = HorizontalPadding,
                        top = 10.dp,
                        end = HorizontalPadding,
                        bottom = 24.dp
                    )
            )
        }
    }
}

@Composable
internal fun ConfirmRemoveDialog(
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
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDialogDismissed(false)
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                Text(stringResource(R.string.wallet_remove_confirmation))
            }
        )
    }
}
