package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.R
import com.stripe.android.uicore.elements.H6Text
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.stripeColors

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun BankAccountElementUI(
    state: BankAccountElement.State,
    enabled: Boolean,
    onRemoveAccount: () -> Unit,
) {
    var openDialog by rememberSaveable { mutableStateOf(false) }
    val bankIcon = remember(state.bankName) {
        // TODO
        R.drawable.stripe_ic_bank
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        H6Text(
            text = stringResource(R.string.stripe_title_bank_account),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(vertical = 12.dp)
                    .padding(start = 16.dp)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Image(
                        painter = painterResource(bankIcon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )

                    Text(
                        text = "${state.bankName} •••• ${state.last4}",
                        modifier = Modifier.alpha(if (enabled) 0.5f else 1f),
                        color = MaterialTheme.stripeColors.onComponent,
                    )
                }

                IconButton(
                    enabled = enabled,
                    onClick = { openDialog = true },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.stripe_ic_clear),
                        contentDescription = null,
                    )
                }
            }
        }

        if (state.showCheckbox) {
            SaveForFutureUseElementUI(
                enabled = true,
                element = state.saveForFutureUseElement,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (openDialog && state.last4 != null) {
        SimpleDialogElementUI(
            titleText = "Remove bank account", // TODO
            messageText = "Bank account ending in ${state.last4}", // TODO
            confirmText = stringResource(
                id = R.string.stripe_remove
            ),
            dismissText = stringResource(
                id = R.string.stripe_cancel
            ),
            destructive = true,
            onConfirmListener = {
                openDialog = false
                onRemoveAccount()
            },
            onDismissListener = {
                openDialog = false
            }
        )
    }
}
