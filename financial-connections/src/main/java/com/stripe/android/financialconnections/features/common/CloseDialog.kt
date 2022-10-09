package com.stripe.android.financialconnections.features.common

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun CloseDialog(
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    AlertDialog(
        backgroundColor = FinancialConnectionsTheme.colors.backgroundContainer,
        onDismissRequest = onDismissClick,
        title = {
            Text(
                text = stringResource(R.string.stripe_close_dialog_title),
                style = FinancialConnectionsTheme.typography.subtitle
            )
        },
        text = {
            Text(
                text = stringResource(R.string.stripe_close_dialog_desc),
                style = FinancialConnectionsTheme.typography.body
            )
        },
        confirmButton = {
            FinancialConnectionsButton(
                size = FinancialConnectionsButton.Size.Pill,
                type = FinancialConnectionsButton.Type.Critical,
                onClick = onConfirmClick
            ) {
                Text(stringResource(R.string.stripe_close_dialog_confirm))
            }
        },
        dismissButton = {
            FinancialConnectionsButton(
                size = FinancialConnectionsButton.Size.Pill,
                type = FinancialConnectionsButton.Type.Secondary,
                onClick = onDismissClick
            ) {
                Text(stringResource(R.string.stripe_close_dialog_back))
            }
        },
    )
}

@Composable
@Preview
internal fun CloseDialogPreview() {
    FinancialConnectionsPreview {
        CloseDialog(
            {}, {}
        )
    }
}
