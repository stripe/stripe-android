package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun CloseDialog(
    description: TextResource,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    AlertDialog(
        shape = RoundedCornerShape(8.dp),
        backgroundColor = FinancialConnectionsTheme.colors.backgroundContainer,
        onDismissRequest = onDismissClick,
        title = {
            Text(
                text = stringResource(R.string.stripe_close_dialog_title),
            )
        },
        text = {
            Text(
                text = description.toText().toString(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = FinancialConnectionsTheme.colors.textCritical
                ),
            ) {
                Text(stringResource(R.string.stripe_close_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = FinancialConnectionsTheme.colors.textPrimary
                ),
            ) {
                Text(stringResource(R.string.stripe_close_dialog_back))
            }
        },
    )
}

@Preview(group = "Close Dialog", name = "Default")
@Composable
internal fun CloseDialogPreview() {
    FinancialConnectionsTheme {
        CloseDialog(
            description = TextResource.StringId(R.string.stripe_close_dialog_desc),
            onConfirmClick = {},
            onDismissClick = {}
        )
    }
}
