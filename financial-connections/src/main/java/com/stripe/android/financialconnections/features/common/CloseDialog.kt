package com.stripe.android.financialconnections.features.common

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun CloseDialog(
    onConfirmClick: () -> Unit
) {
    AlertDialog(
        backgroundColor = FinancialConnectionsTheme.colors.backgroundContainer,
        onDismissRequest = {},
        title = {
            Text(
                text = "Are you sure you want to cancel?",
                style = FinancialConnectionsTheme.typography.body
            )
        },
        text = {
            Text(
                text = "TBD",
                style = FinancialConnectionsTheme.typography.body
            )
        },
        confirmButton = {
            FinancialConnectionsButton(
                size = FinancialConnectionsButton.Size.Pill,
                type = FinancialConnectionsButton.Type.Critical,
                onClick = onConfirmClick
            ) {
                Text("This is the Confirm Button")
            }
        },
        dismissButton = {
            FinancialConnectionsButton(
                size = FinancialConnectionsButton.Size.Pill,
                type = FinancialConnectionsButton.Type.Secondary,
                onClick = {}
            ) {
                Text("This is the dismiss Button")
            }
        }
    )
}
