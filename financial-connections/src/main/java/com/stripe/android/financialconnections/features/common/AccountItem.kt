package com.stripe.android.financialconnections.features.common

import FinancialConnectionsGenericInfoScreen
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat.getLocales
import com.stripe.android.financialconnections.features.common.AccountSelectionState.Disabled
import com.stripe.android.financialconnections.features.common.AccountSelectionState.Enabled
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.NetworkedAccount
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.clickableSingle
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
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
internal fun AccountItem(
    selected: Boolean,
    showInstitutionIcon: Boolean = true,
    onAccountClicked: (PartnerAccount) -> Unit,
    account: PartnerAccount,
    networkedAccount: NetworkedAccount? = null,
) {
    val view = LocalView.current
    val viewState = remember(account, networkedAccount) { getVisibilityState(account, networkedAccount) }

    val shape = remember { RoundedCornerShape(12.dp) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = when {
                    selected -> colors.primary
                    else -> colors.borderNeutral
                },
                shape = shape
            )
            .clickableSingle(enabled = viewState != Disabled) {
                if (SDK_INT >= M) view.performHapticFeedback(CONTEXT_CLICK)
                onAccountClicked(account)
            }
            .alpha(viewState.alpha)
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (networkedAccount?.accountIcon ?: account.institution?.icon)
                ?.default
                ?.takeIf { showInstitutionIcon }?.let {
                    InstitutionIcon(institutionIcon = it)
                }
            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    text = account.name,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = colors.textDefault,
                    style = typography.labelLargeEmphasized
                )
                AccountSubtitle(viewState, account, networkedAccount)
            }
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .alpha(if (selected) 1f else 0f),
                imageVector = Icons.Default.Check,
                tint = colors.primary,
                contentDescription = "Selected"
            )
        }
    }
}

private fun getVisibilityState(
    account: PartnerAccount,
    networkedAccount: NetworkedAccount?
): AccountSelectionState = when {
    // networked account's allowSelection takes precedence over the account's.
    networkedAccount?.allowSelection ?: account.allowSelection -> Enabled
    // Even if the account looks "not selectable", when clicking we'd display the "drawer on selection" if available.
    networkedAccount?.drawerOnSelection != null -> AccountSelectionState.VisuallyDisabled
    else -> Disabled
}

@Composable
private fun AccountSubtitle(
    accountSelectionState: AccountSelectionState,
    account: PartnerAccount,
    networkedAccount: NetworkedAccount?,
) {
    val subtitle = getSubtitle(
        accountSelectionState = accountSelectionState,
        account = account,
        networkedAccount = networkedAccount
    )
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiddleEllipsisText(
            text = subtitle ?: account.redactedAccountNumbers,
            // underline there's a subtitle and the account is clickable (even if visually disabled)
            textDecoration = if (accountSelectionState != Disabled && subtitle != null) {
                TextDecoration.Underline
            } else {
                null
            },
            color = colors.textSubdued,
            style = typography.labelMedium
        )
        account.getFormattedBalance()
            // Only show balance if there is no custom subtitle (e.g. "Account unavailable, Repair account, etc")
            ?.takeIf { subtitle == null }
            ?.let {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = it,
                    color = colors.textSubdued,
                    style = typography.labelSmall,
                    modifier = Modifier
                        .background(
                            color = colors.backgroundSecondary,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)

                )
            }
    }
}

@Composable
private fun getSubtitle(
    accountSelectionState: AccountSelectionState,
    account: PartnerAccount,
    networkedAccount: NetworkedAccount?,
): String? = when {
    networkedAccount?.caption != null -> networkedAccount.caption
    accountSelectionState != Enabled && account.allowSelectionMessage?.isNotBlank() == true ->
        account.allowSelectionMessage
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

private enum class AccountSelectionState(val alpha: Float) {
    Enabled(alpha = 1f),
    Disabled(alpha = VISUALLY_DISABLED_ALPHA),
    VisuallyDisabled(alpha = VISUALLY_DISABLED_ALPHA)
}

/**
 * Using a more visible alpha (instead of ContentAlpha.disabled)
 * since disabled accounts are still clickable (they can show a drawer on selection)
 */
private const val VISUALLY_DISABLED_ALPHA = 0.6f

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
                        name = "Regular Checking",
                        allowSelectionMessage = "allowSelectionMessage",
                        institution = FinancialConnectionsInstitution(
                            id = "id",
                            name = "Bank of America",
                            featured = false,
                            mobileHandoffCapable = false,
                            icon = Image(default = "www.image.com")
                        ),
                        authorization = "",
                        currency = "USD",
                        category = FinancialConnectionsAccount.Category.CASH,
                        subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                        supportedPaymentMethodTypes = emptyList(),
                        balanceAmount = 100,
                    ),
                    networkedAccount = NetworkedAccount(
                        id = "id",
                        caption = "With some caption",
                        allowSelection = true,
                        icon = Image(default = "www.image.com")
                    )
                )
                AccountItem(
                    selected = false,
                    onAccountClicked = { },
                    account = PartnerAccount(
                        id = "id",
                        name = "Regular Checking",
                        allowSelectionMessage = "allowSelectionMessage",
                        institution = FinancialConnectionsInstitution(
                            id = "id",
                            name = "Bank of America",
                            featured = false,
                            mobileHandoffCapable = false,
                            icon = Image(default = "www.image.com")
                        ),
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
                        institution = FinancialConnectionsInstitution(
                            id = "id",
                            name = "Bank of America",
                            featured = false,
                            mobileHandoffCapable = false,
                            icon = Image(default = "www.image.com")
                        ),
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
                        name = "Regular Checking (Disabled)",
                        _allowSelection = false,
                        allowSelectionMessage = null,
                        institution = FinancialConnectionsInstitution(
                            id = "id",
                            name = "Bank of America",
                            featured = false,
                            mobileHandoffCapable = false,
                            icon = Image(default = "www.image.com")
                        ),
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
                        name = "Regular Checking (Disabled)",
                        _allowSelection = false,
                        allowSelectionMessage = "Unselectable with custom message",
                        institution = FinancialConnectionsInstitution(
                            id = "id",
                            name = "Bank of America",
                            featured = false,
                            mobileHandoffCapable = false,
                            icon = Image(default = "www.image.com")
                        ),
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
                        name = "Manually entered (Disabled)",
                        _allowSelection = false,
                        allowSelectionMessage = "Visually disabled but clickable",
                        institution = FinancialConnectionsInstitution(
                            id = "id",
                            name = "Bank of America",
                            featured = false,
                            mobileHandoffCapable = false,
                            icon = Image(default = "www.image.com")
                        ),
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
                        icon = Image(default = "www.image.com"),
                        drawerOnSelection = FinancialConnectionsGenericInfoScreen(
                            id = "id",
                        )
                    )
                )
            }
        }
    }
}
