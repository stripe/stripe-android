package com.stripe.android.financialconnections.features.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.format.CurrencyFormatter
import com.stripe.android.uicore.text.MiddleEllipsisText
import java.util.Locale

@Composable
@Suppress("MagicNumber")
internal fun AccountItem(
    selected: Boolean,
    onAccountClicked: (PartnerAccount) -> Unit,
    account: PartnerAccount,
    selectorContent: @Composable RowScope.() -> Unit
) {
    val verticalPadding =
        remember(account) { if (account.displayableAccountNumbers != null) 10.dp else 12.dp }
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
            .clickable(enabled = account.allowSelection) { onAccountClicked(account) }
            .padding(vertical = verticalPadding, horizontal = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            selectorContent()
            Spacer(modifier = Modifier.size(16.dp))
            val (title, subtitle) = getAccountTexts(account = account)
            Column(
                Modifier.weight(0.7f)
            ) {
                Text(
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = if (account.allowSelection) {
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
        }
    }
}

@Composable
private fun getAccountTexts(
    account: PartnerAccount,
): Pair<String, String?> {
    val formattedBalance = account.getFormattedBalance()
    val title = when {
        account.allowSelection.not() ||
            formattedBalance != null -> "${account.name} ${account.encryptedNumbers}"

        else -> account.name
    }
    val subtitle = when {
        account.allowSelection.not() -> account.allowSelectionMessage
        formattedBalance != null -> formattedBalance
        account.encryptedNumbers.isNotEmpty() -> account.encryptedNumbers
        else -> null
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
