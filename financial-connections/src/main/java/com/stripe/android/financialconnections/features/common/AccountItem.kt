package com.stripe.android.financialconnections.features.common

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.view.HapticFeedbackConstants.CONTEXT_CLICK
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat.getLocales
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.NetworkedAccount
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.clickableSingle
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import com.stripe.android.uicore.format.CurrencyFormatter
import com.stripe.android.uicore.text.MiddleEllipsisText
import java.util.Locale

/**
 * A single account item in an account picker list.
 *
 * @param selected whether this account is selected
 * @param onAccountClicked callback when this account is clicked
 * @param account the account info to display
 * @param networkedAccount For networked accounts, extra info to display
 */
@Composable
@Suppress("LongMethod")
internal fun AccountItem(
    selected: Boolean,
    onAccountClicked: (PartnerAccount) -> Unit,
    account: PartnerAccount,
    networkedAccount: NetworkedAccount? = null,
) {
    val view = LocalView.current
    // networked account's allowSelection takes precedence over the account's.
    val selectable = networkedAccount?.allowSelection ?: account.allowSelection
    val shape = remember { RoundedCornerShape(16.dp) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = when {
                    selected -> v3Colors.borderBrand
                    else -> v3Colors.border
                },
                shape = shape
            )
            .clickableSingle(enabled = selectable) {
                if (SDK_INT >= M) view.performHapticFeedback(CONTEXT_CLICK)
                onAccountClicked(account)
            }
            .alpha(if (selectable) ContentAlpha.high else ContentAlpha.disabled)
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            networkedAccount?.icon?.default?.let {
                InstitutionIcon(institutionIcon = it)
            }
            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    text = account.name,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = v3Colors.textDefault,
                    style = v3Typography.labelLargeEmphasized
                )
                AccountSubtitle(selectable, account, networkedAccount)
            }
            Icon(
                modifier = Modifier
                    .size(16.dp)
                    .alpha(if (selected) ContentAlpha.high else ContentAlpha.disabled),
                imageVector = Icons.Default.Check,
                tint = v3Colors.iconBrand,
                contentDescription = "Selected"
            )
        }
    }
}

@Composable
private fun AccountSubtitle(
    selectable: Boolean,
    account: PartnerAccount,
    networkedAccount: NetworkedAccount?,
) {
    val subtitle = getSubtitle(allowSelection = selectable, account = account, networkedAccount = networkedAccount)
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiddleEllipsisText(
            text = subtitle ?: account.redactedAccountNumbers,
            color = v3Colors.textSubdued,
            style = v3Typography.labelMedium
        )
        account.getFormattedBalance()
            // Only show balance if there is no custom subtitle (e.g. "Account unavailable, Repair account, etc")
            ?.takeIf { subtitle == null }
            ?.let {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = it,
                    color = v3Colors.textSubdued,
                    style = v3Typography.labelSmall,
                    modifier = Modifier
                        .background(
                            color = v3Colors.backgroundOffset,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)

                )
            }
    }
}

@Composable
private fun getSubtitle(
    allowSelection: Boolean,
    account: PartnerAccount,
    networkedAccount: NetworkedAccount?,
): String? = when {
    networkedAccount?.caption != null -> networkedAccount.caption
    allowSelection.not() && account.allowSelectionMessage?.isNotBlank() == true -> account.allowSelectionMessage
    else -> null
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

@Suppress("LongMethod")
@Composable
@Preview
internal fun AccountItemPreview() {
    FinancialConnectionsPreview {
        FinancialConnectionsScaffold(topBar = { }) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AccountItem(
                    selected = false,
                    onAccountClicked = { },
                    account = PartnerAccount(
                        id = "id",
                        name = "Regular Checking (Unselected)",
                        allowSelectionMessage = "allowSelectionMessage",
                        authorization = "",
                        currency = "USD",
                        category = FinancialConnectionsAccount.Category.CASH,
                        subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                        supportedPaymentMethodTypes = emptyList(),
                        balanceAmount = 100,
                    ),
                )
                AccountItem(
                    selected = true,
                    onAccountClicked = { },
                    account = PartnerAccount(
                        id = "id",
                        name = "Regular Checking (Selected)",
                        allowSelectionMessage = "allowSelectionMessage",
                        authorization = "",
                        currency = "USD",
                        category = FinancialConnectionsAccount.Category.CASH,
                        subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                        supportedPaymentMethodTypes = emptyList(),
                        balanceAmount = 100,
                    ),
                    networkedAccount = NetworkedAccount(
                        id = "id",
                        allowSelection = true,
                        icon = Image(default = "www.image.com")
                    )
                )
                AccountItem(
                    selected = false,
                    onAccountClicked = { },
                    account = PartnerAccount(
                        id = "id",
                        name = "Regular Checking (Selected)",
                        _allowSelection = false,
                        allowSelectionMessage = null,
                        authorization = "",
                        currency = "USD",
                        category = FinancialConnectionsAccount.Category.CASH,
                        subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                        supportedPaymentMethodTypes = emptyList(),
                        balanceAmount = 100,
                    ),
                    networkedAccount = NetworkedAccount(
                        id = "id",
                        allowSelection = false,
                        icon = Image(default = "www.image.com")
                    )
                )
                AccountItem(
                    selected = false,
                    onAccountClicked = { },
                    account = PartnerAccount(
                        id = "id",
                        name = "Regular Checking (Selected)",
                        _allowSelection = false,
                        allowSelectionMessage = "Unselectable with custom message",
                        authorization = "",
                        currency = "USD",
                        category = FinancialConnectionsAccount.Category.CASH,
                        subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                        supportedPaymentMethodTypes = emptyList(),
                        balanceAmount = 100,
                    ),
                    networkedAccount = NetworkedAccount(
                        id = "id",
                        allowSelection = false,
                        icon = Image(default = "www.image.com")
                    )
                )
            }
        }
    }
}
