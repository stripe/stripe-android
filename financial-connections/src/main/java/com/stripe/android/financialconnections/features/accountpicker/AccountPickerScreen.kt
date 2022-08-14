package com.stripe.android.financialconnections.features.accountpicker

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.institutionpicker.LoadingContent
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun AccountPickerScreen() {
    val viewModel: AccountPickerViewModel = mavericksViewModel()
    val state: State<AccountPickerState> = viewModel.collectAsState()
    AccountPickerContent(
        state = state.value,
        onAccountClicked = viewModel::onAccountClicked,
        onSelectAccounts = viewModel::selectAccounts
    )
}

@Composable
private fun AccountPickerContent(
    state: AccountPickerState,
    onAccountClicked: (PartnerAccount) -> Unit,
    onSelectAccounts: () -> Unit
) {
    FinancialConnectionsScaffold {
        when (val accounts = state.accounts) {
            Uninitialized, is Loading -> LoadingContent(
                R.string.stripe_account_picker_loading_title,
                R.string.stripe_account_picker_loading_desc
            )
            is Success -> AccountPickerLoaded(
                loading = state.isLoading,
                accounts = accounts(),
                selectedIds = state.selectedIds,
                onAccountClicked = onAccountClicked,
                onSelectAccounts = onSelectAccounts
            )
            is Fail -> UnclassifiedErrorContent()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AccountPickerLoaded(
    loading: Boolean,
    accounts: PartnerAccountsList,
    selectedIds: Set<String>,
    onAccountClicked: (PartnerAccount) -> Unit,
    onSelectAccounts: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(R.string.stripe_account_picker_multiselect_account),
            style = FinancialConnectionsTheme.typography.subtitle
        )
        Spacer(modifier = Modifier.size(24.dp))
        LazyColumn(
            contentPadding = PaddingValues(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts.data, key = { it.id }) { account ->
                MultiSelectAccount(
                    selected = selectedIds.contains(account.id),
                    onAccountClicked = onAccountClicked,
                    account = account
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        FinancialConnectionsButton(
            loading = loading,
            onClick = onSelectAccounts,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.stripe_account_picker_confirm,
                    selectedIds.size
                )
            )
        }
    }
}

@Composable
private fun MultiSelectAccount(
    selected: Boolean,
    onAccountClicked: (PartnerAccount) -> Unit,
    account: PartnerAccount
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (selected) {
                    FinancialConnectionsTheme.colors.textBrand
                } else {
                    FinancialConnectionsTheme.colors.borderDefault
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onAccountClicked(account) }
            .padding(12.dp)
    ) {

        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                colors = CheckboxDefaults.colors(
                    checkedColor = FinancialConnectionsTheme.colors.textBrand,
                    checkmarkColor = FinancialConnectionsTheme.colors.textWhite,
                    uncheckedColor = FinancialConnectionsTheme.colors.borderDefault
                ),
                onCheckedChange = null
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(
                    text = account.name,
                    color = FinancialConnectionsTheme.colors.textPrimary,
                    style = FinancialConnectionsTheme.typography.bodyEmphasized,
                )
                account.displayableAccountNumbers?.let {
                    Text(
                        text = "****$it",
                        color = FinancialConnectionsTheme.colors.textSecondary,
                        style = FinancialConnectionsTheme.typography.captionTight,
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    group = "Account Picker Pane",
    name = "One account selected"
)
@Composable
internal fun AccountPickerPreview() {
    FinancialConnectionsTheme {
        AccountPickerContent(
            AccountPickerState(
                selectedIds = setOf("id1"),
                accounts = Success(
                    PartnerAccountsList(
                        data = listOf(
                            PartnerAccount(
                                authorization = "Authorization",
                                category = FinancialConnectionsAccount.Category.CASH,
                                id = "id1",
                                name = "Account 1",
                                balanceAmount = 1000,
                                displayableAccountNumbers = "1234",
                                currency = "$",
                                subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                                supportedPaymentMethodTypes = emptyList(),
                            ),
                            PartnerAccount(
                                authorization = "Authorization",
                                category = FinancialConnectionsAccount.Category.CASH,
                                id = "id2",
                                name = "Account 2",
                                subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
                                supportedPaymentMethodTypes = emptyList()
                            )
                        ),
                        hasMore = false,
                        nextPane = FinancialConnectionsSessionManifest.NextPane.ACCOUNT_PICKER,
                        url = ""
                    )
                )
            ),
            onAccountClicked = {},
            onSelectAccounts = {}
        )
    }
}
