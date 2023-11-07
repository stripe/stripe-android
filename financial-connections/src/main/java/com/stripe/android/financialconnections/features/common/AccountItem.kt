package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat.getLocales
import com.stripe.android.financialconnections.model.NetworkedAccount
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.components.clickableSingle
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.format.CurrencyFormatter
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.text.MiddleEllipsisText
import java.util.Locale

/**
 * A single account item in an account picker list.
 *
 * @param selected whether this account is selected
 * @param onAccountClicked callback when this account is clicked
 * @param account the account info to display
 * @param networkedAccount For networked accounts, extra info to display
 * @param selectorContent content to display on the left side of the account item
 */
@Composable
@Suppress("LongMethod")
internal fun AccountItem(
    selected: Boolean,
    onAccountClicked: (PartnerAccount) -> Unit,
    account: PartnerAccount,
    networkedAccount: NetworkedAccount? = null,
    selectorContent: @Composable RowScope.() -> Unit
) {
    // networked account's allowSelection takes precedence over the account's.
    val selectable = networkedAccount?.allowSelection ?: account.allowSelection
    val (title, subtitle) = getAccountTexts(
        account = account,
        networkedAccount = networkedAccount,
        allowSelection = selectable
    )
    val verticalPadding = remember(account) { if (subtitle != null) 10.dp else 12.dp }
    val shape = remember { RoundedCornerShape(8.dp) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = when {
                    selected -> FinancialConnectionsTheme.colors.textBrand
                    else -> FinancialConnectionsTheme.colors.borderDefault
                },
                shape = shape
            )
            .clickableSingle(enabled = selectable) { onAccountClicked(account) }
            .padding(vertical = verticalPadding, horizontal = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            selectorContent()
            Spacer(modifier = Modifier.size(16.dp))
            Column(
                Modifier.weight(ACCOUNT_COLUMN_WEIGHT)
            ) {
                Text(
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = if (selectable) {
                        FinancialConnectionsTheme.colors.textPrimary
                    } else {
                        FinancialConnectionsTheme.colors.textDisabled
                    },
                    style = FinancialConnectionsTheme.typography.bodyEmphasized
                )
                subtitle?.let {
                    Spacer(modifier = Modifier.size(4.dp))
                    MiddleEllipsisText(
                        text = it,
                        color = FinancialConnectionsTheme.colors.textDisabled,
                        style = FinancialConnectionsTheme.typography.captionTight
                    )
                }
            }
            networkedAccount?.icon?.default?.let {
                StripeImage(
                    url = it,
                    imageLoader = LocalImageLoader.current,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun getAccountTexts(
    allowSelection: Boolean,
    account: PartnerAccount,
    networkedAccount: NetworkedAccount?,
): Pair<String, String?> {
    val formattedBalance = account.getFormattedBalance()

    val subtitle = when {
        networkedAccount?.caption != null -> networkedAccount.caption
        allowSelection.not() && account.allowSelectionMessage?.isNotBlank() == true ->
            account.allowSelectionMessage

        formattedBalance != null -> formattedBalance
        account.redactedAccountNumbers != null -> account.redactedAccountNumbers
        else -> null
    }

    // just show redacted account numbers in the title if they're not in the subtitle.
    val title = when {
        subtitle != account.redactedAccountNumbers -> listOfNotNull(
            account.name,
            account.redactedAccountNumbers
        ).joinToString(" ")

        else -> account.name
    }

    return title to subtitle
}

@Composable
private fun PartnerAccount.getFormattedBalance(): String? {
    val locale = getLocales(LocalConfiguration.current).get(0) ?: Locale.getDefault()
    val debugMode = LocalInspectionMode.current
    if (balanceAmount == null || currency == null) return null
    return when {
        debugMode -> ("$currency$balanceAmount")
        else -> CurrencyFormatter.format(
            amount = balanceAmount.toLong(),
            amountCurrencyCode = currency,
            targetLocale = locale
        )
    }
}

private const val ACCOUNT_COLUMN_WEIGHT = 0.7f
