package com.stripe.android.link.ui.wallet

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.R
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.linkColors
import com.stripe.android.model.ConsumerPaymentDetails

private val BottomSheetFirstItemModifier = Modifier.padding(
    start = HorizontalPadding,
    top = 24.dp,
    end = HorizontalPadding,
    bottom = 10.dp
)

private val BottomSheetMiddleItemModifier = Modifier.padding(
    horizontal = HorizontalPadding,
    vertical = 10.dp
)

private val BottomSheetLastItemModifier = Modifier.padding(
    start = HorizontalPadding,
    top = 10.dp,
    end = HorizontalPadding,
    bottom = 24.dp
)

@Preview
@Composable
internal fun WalletBottomSheetContent() {
    WalletBottomSheetContent(
        paymentDetails = ConsumerPaymentDetails.BankAccount(
            id = "id",
            isDefault = true,
            bankIconCode = null,
            bankName = "Bank Name",
            last4 = "last4"
        ),
        onCancelClick = {},
        onEditClick = {},
        onRemoveClick = {}
    )
}

@Composable
internal fun WalletBottomSheetContent(
    paymentDetails: ConsumerPaymentDetails.PaymentDetails,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    BottomSheetContent(
        removeLabel = paymentDetails.removeLabel,
        onRemoveClick = onRemoveClick,
        onCancelClick = onCancelClick,
        onEditClick = onEditClick.takeIf { paymentDetails is ConsumerPaymentDetails.Card }
    )
}

@Composable
private fun BottomSheetContent(
    @StringRes removeLabel: Int,
    onRemoveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onEditClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        onEditClick?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = it)
            ) {
                Text(
                    text = stringResource(R.string.wallet_update_card),
                    modifier = BottomSheetFirstItemModifier
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onRemoveClick)
        ) {
            Text(
                text = stringResource(removeLabel),
                modifier = onEditClick?.let { BottomSheetMiddleItemModifier }
                    ?: BottomSheetFirstItemModifier
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCancelClick)
        ) {
            Text(
                text = stringResource(R.string.cancel),
                modifier = BottomSheetLastItemModifier
            )
        }
    }
}

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

private val ConsumerPaymentDetails.PaymentDetails.removeLabel
    get() = when (this) {
        is ConsumerPaymentDetails.Card -> R.string.wallet_remove_card
        is ConsumerPaymentDetails.BankAccount -> R.string.wallet_remove_linked_account
    }

private val ConsumerPaymentDetails.PaymentDetails.removeConfirmation
    get() = when (this) {
        is ConsumerPaymentDetails.Card -> R.string.wallet_remove_card_confirmation
        is ConsumerPaymentDetails.BankAccount -> R.string.wallet_remove_account_confirmation
    }
