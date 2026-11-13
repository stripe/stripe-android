package com.stripe.android.paymentsheet.stripebalance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.RowButton
import com.stripe.android.ui.core.elements.RenderableFormElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.H6Text
import com.stripe.android.uicore.format.CurrencyFormatter
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lists the buyer's financial accounts so they can choose which one to pay from with Stripe balance.
 * Accounts that can't cover the payment are shown with the reason, but can't be selected. Nothing is selected until
 * the buyer chooses, so the form is incomplete until then.
 *
 * @param financialAccounts The buyer's accounts, loaded with the Payment Element.
 * @param initialFinancialAccountId The account to select if it's selectable, e.g. the buyer's previous choice.
 */
internal class StripeBalanceFinancialAccountsElement(
    private val financialAccounts: StripeBalanceFinancialAccounts,
    initialFinancialAccountId: String?,
) : RenderableFormElement(
    identifier = FormFieldId.Generic("stripe_balance_financial_accounts"),
    allowsUserInteraction = true,
) {
    private val accounts = financialAccounts.accounts

    private val selectedFinancialAccountId = MutableStateFlow(
        initialFinancialAccountId?.takeIf { id -> accounts.any { it.id == id && it.isSelectable } }
    )

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<FormFieldId, FormFieldEntry>>> {
        return selectedFinancialAccountId.mapAsStateFlow { id ->
            listOf(FINANCIAL_ACCOUNT_FIELD_ID to FormFieldEntry(value = id, isComplete = id != null))
        }
    }

    fun select(financialAccount: StripeBalanceFinancialAccount) {
        if (financialAccount.isSelectable) {
            selectedFinancialAccountId.value = financialAccount.id
        }
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        hiddenIdentifiers: Set<FormFieldId>,
        lastTextFieldIdentifier: FormFieldId?,
    ) {
        val selectedId by selectedFinancialAccountId.collectAsState()
        StripeBalanceFinancialAccountsUI(
            accounts = accounts,
            currency = financialAccounts.currency,
            selectedFinancialAccountId = selectedId,
            enabled = enabled,
            onSelect = ::select,
        )
    }

    companion object {
        val FINANCIAL_ACCOUNT_FIELD_ID = FormFieldId.Generic("stripe_balance[financial_account]")
    }
}

@Composable
internal fun StripeBalanceFinancialAccountsUI(
    accounts: List<StripeBalanceFinancialAccount>,
    currency: String?,
    selectedFinancialAccountId: String?,
    enabled: Boolean,
    onSelect: (StripeBalanceFinancialAccount) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Asks the buyer to choose, so the rows read as options rather than information
        H6Text(
            text = stringResource(R.string.stripe_paymentsheet_stripe_balance_pay_from),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            accounts.forEach { account ->
                StripeBalanceFinancialAccountRow(
                    financialAccount = account,
                    currency = currency,
                    isSelected = account.id == selectedFinancialAccountId,
                    enabled = enabled,
                    onClick = { onSelect(account) },
                )
            }
        }
    }
}

/**
 * A selectable row with a radio button, the account's name, and its available balance in the payment's currency.
 * Styled like the Payment Element's floating payment method rows.
 */
@Composable
private fun StripeBalanceFinancialAccountRow(
    financialAccount: StripeBalanceFinancialAccount,
    currency: String?,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val isEnabled = enabled && financialAccount.isSelectable
    RowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        onClick = onClick,
        contentPaddingValues = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = isSelected,
                // The row handles selection
                onClick = null,
                enabled = isEnabled,
                modifier = Modifier.size(20.dp),
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colors.primary,
                    unselectedColor = MaterialTheme.stripeColors.componentBorder,
                ),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = financialAccount.displayName ?: stringResource(
                        R.string.stripe_paymentsheet_stripe_balance_unnamed_account,
                        financialAccount.id.takeLast(UNNAMED_ACCOUNT_ID_SUFFIX_LENGTH),
                    ),
                    style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.stripeColors.onComponent,
                )
                StripeBalanceFinancialAccountSubtitle(financialAccount = financialAccount, currency = currency)
            }
        }
    }
}

/**
 * The available balance, after why the account can't be selected if it can't, as in
 * "Insufficient balance · £25.00 available".
 */
@Composable
private fun StripeBalanceFinancialAccountSubtitle(
    financialAccount: StripeBalanceFinancialAccount,
    currency: String?,
) {
    val reason = financialAccount.ineligibleReason
        ?.takeIf { !financialAccount.isSelectable }
        ?.let { reason ->
            when (reason) {
                StripeBalanceFinancialAccount.IneligibleReason.InsufficientBalance ->
                    stringResource(R.string.stripe_paymentsheet_stripe_balance_insufficient_balance)
                StripeBalanceFinancialAccount.IneligibleReason.CurrencyMismatch ->
                    stringResource(R.string.stripe_paymentsheet_stripe_balance_currency_not_supported)
                // A reason this SDK doesn't know about disables the account without a label
                is StripeBalanceFinancialAccount.IneligibleReason.Other -> null
            }
        }
    // Like web, the balance only shows in the payment's currency, so not when setting up
    val availableBalance = currency?.let { financialAccount.availableBalance(it) }?.let { amount ->
        stringResource(
            R.string.stripe_paymentsheet_stripe_balance_available,
            CurrencyFormatter.format(amount, currency),
        )
    }
    if (reason == null && availableBalance == null) {
        return
    }

    val errorColor = MaterialTheme.colors.error
    Text(
        text = buildAnnotatedString {
            reason?.let { withStyle(SpanStyle(color = errorColor)) { append(it) } }
            if (reason != null && availableBalance != null) {
                append(" · ")
            }
            availableBalance?.let(::append)
        },
        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Normal),
        color = MaterialTheme.stripeColors.subtitle,
    )
}

private const val UNNAMED_ACCOUNT_ID_SUFFIX_LENGTH = 4
