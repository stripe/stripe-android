package com.stripe.link.feature.enableactivity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.link.core.ui.component.LinkSwitch
import com.stripe.link.core.ui.component.ListItem
import com.stripe.link.core.ui.component.ListItemConfig

@Composable
fun EnableActivityScreen(
    accounts: List<FinancialConnectionsAccount>,
    onToggle: (String, Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        accounts.forEach { account ->
            FinancialConnectionsAccountRow(
                account = account,
                onToggle = onToggle,
            )
        }
    }
}

@Composable
internal fun FinancialConnectionsAccountRow(
    account: FinancialConnectionsAccount,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The row itself is non-interactive — only the LinkSwitch responds to taps.
    ListItem(
        label = account.accountName,
        modifier = modifier,
        sublabel = account.institutionName,
        useInsetStyle = true,
        leadingIcon = null,
        detailsContent = {
            LinkSwitch(
                checked = account.isSubscribed,
                onCheckedChange = if (account.canToggle) {
                    { enabled -> onToggle(account.id, enabled) }
                } else {
                    null
                },
            )
        },
        config = ListItemConfig(),
    )
}

// ---------------------------------------------------------------------------
// Domain model
// ---------------------------------------------------------------------------

data class FinancialConnectionsAccount(
    val id: String,
    val institutionName: String,
    val accountName: String,
    val canToggle: Boolean,
    val isSubscribed: Boolean,
)
