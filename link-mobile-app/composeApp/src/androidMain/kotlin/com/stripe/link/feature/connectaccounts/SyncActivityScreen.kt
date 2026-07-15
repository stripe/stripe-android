package com.stripe.link.feature.connectaccounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.stripe.link.core.ui.component.LinkSwitch
import com.stripe.link.core.ui.component.ListItem
import com.stripe.link.core.ui.component.ListItemConfig

@Composable
fun SyncActivityScreen(
    accounts: List<SyncActivityAccount>,
    onToggle: (String, Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        accounts.forEach { account ->
            SyncActivityAccountRow(
                account = account,
                onToggle = onToggle,
            )
        }
    }
}

@Composable
internal fun SyncActivityAccountRow(
    account: SyncActivityAccount,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEnabled by remember(account.isEnabled) { mutableStateOf(account.isEnabled) }

    // The row itself is non-interactive — only the LinkSwitch responds to taps.
    ListItem(
        label = account.name,
        modifier = modifier,
        sublabel = account.sublabel,
        useInsetStyle = true,
        detailsContent = {
            LinkSwitch(
                checked = isEnabled,
                onCheckedChange = { enabled -> onToggle(account.id, enabled) },
            )
        },
        config = ListItemConfig(),
    )
}

// ---------------------------------------------------------------------------
// Domain model
// ---------------------------------------------------------------------------

data class SyncActivityAccount(
    val id: String,
    val name: String,
    val sublabel: String? = null,
    val isEnabled: Boolean,
)
