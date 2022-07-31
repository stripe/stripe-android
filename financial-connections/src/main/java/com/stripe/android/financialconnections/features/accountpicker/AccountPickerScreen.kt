package com.stripe.android.financialconnections.features.accountpicker

import androidx.compose.foundation.background
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
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
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
                onSelectAccounts = onSelectAccounts,
                selectionMode = state.selectionMode
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
    selectionMode: AccountPickerState.SelectionMode,
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
        when (selectionMode) {
            AccountPickerState.SelectionMode.DROPDOWN -> DropdownContent(
                accounts = accounts,
                selectedIds = selectedIds,
                onAccountClicked = onAccountClicked
            )
            AccountPickerState.SelectionMode.RADIO -> SingleSelectContent(
                accounts = accounts,
                selectedIds = selectedIds,
                onAccountClicked = onAccountClicked
            )
            AccountPickerState.SelectionMode.CHECKBOXES -> MultiSelectContent(
                accounts = accounts,
                selectedIds = selectedIds,
                onAccountClicked = onAccountClicked
            )
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DropdownContent(
    accounts: PartnerAccountsList,
    selectedIds: Set<String>,
    onAccountClicked: (PartnerAccount) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOptionText: String? = remember(selectedIds) {
        accounts.data
            .firstOrNull { selectedIds.contains(it.id) }
            ?.let { "${it.name} ${it.numbers}" }
    }
    Spacer(modifier = Modifier.size(12.dp))
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        FinancialConnectionsOutlinedTextField(
            readOnly = true,
            value = selectedOptionText
                ?: stringResource(id = R.string.stripe_account_picker_dropdown_hint),
            onValueChange = { },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            modifier = Modifier.background(
                color = FinancialConnectionsTheme.colors.backgroundSurface
            ),
            onDismissRequest = {
                expanded = false
            }
        ) {
            accounts.data.forEach { selectedAccount ->
                DropdownMenuItem(
                    onClick = {
                        onAccountClicked(selectedAccount)
                        expanded = false
                    }
                ) {
                    val numbers =
                        Text(
                            text = "${selectedAccount.name} ${selectedAccount.numbers}",
                            color = FinancialConnectionsTheme.colors.textPrimary,
                            style = FinancialConnectionsTheme.typography.bodyEmphasized
                        )
                }
            }
        }
    }
}

private val PartnerAccount.numbers get() = displayableAccountNumbers?.let { "••••$it" } ?: ""

@Composable
private fun SingleSelectContent(
    accounts: PartnerAccountsList,
    selectedIds: Set<String>,
    onAccountClicked: (PartnerAccount) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(accounts.data, key = { it.id }) { account ->
            AccountItem(
                selected = selectedIds.contains(account.id),
                onAccountClicked = onAccountClicked,
                account = account
            ) {
                RadioButton(
                    selected = selectedIds.contains(account.id),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = FinancialConnectionsTheme.colors.textBrand,
                        unselectedColor = FinancialConnectionsTheme.colors.borderDefault,
                        disabledColor = FinancialConnectionsTheme.colors.textDisabled
                    ),
                    onClick = null
                )
            }
        }
    }
}

@Composable
private fun MultiSelectContent(
    accounts: PartnerAccountsList,
    selectedIds: Set<String>,
    onAccountClicked: (PartnerAccount) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(accounts.data, key = { it.id }) { account ->
            AccountItem(
                selected = selectedIds.contains(account.id),
                onAccountClicked = onAccountClicked,
                account = account
            ) {
                Checkbox(
                    checked = selectedIds.contains(account.id),
                    colors = CheckboxDefaults.colors(
                        checkedColor = FinancialConnectionsTheme.colors.textBrand,
                        checkmarkColor = FinancialConnectionsTheme.colors.textWhite,
                        uncheckedColor = FinancialConnectionsTheme.colors.borderDefault
                    ),
                    onCheckedChange = null
                )
            }
        }
    }
}

@Composable
private fun AccountItem(
    selected: Boolean,
    onAccountClicked: (PartnerAccount) -> Unit,
    account: PartnerAccount,
    selectorContent: @Composable () -> Unit
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
            selectorContent()
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
    name = "Multiselect - account selected"
)
@Composable
internal fun AccountPickerPreviewMultiSelect() {
    FinancialConnectionsTheme {
        AccountPickerContent(
            AccountPickerStates.multiSelect(),
            onAccountClicked = {}
        )
    }
}

@Preview(
    showBackground = true,
    group = "Account Picker Pane",
    name = "Single select - account selected"
)
@Composable
internal fun AccountPickerPreviewSingleSelect() {
    FinancialConnectionsTheme {
        AccountPickerContent(
            AccountPickerStates.singleSelect(),
            onAccountClicked = {}
        )
    }
}

@Preview(
    showBackground = true,
    group = "Account Picker Pane",
    name = "Dropdown - account selected"
)
@Composable
internal fun AccountPickerPreviewDropdown() {
    FinancialConnectionsTheme {
        AccountPickerContent(
            AccountPickerStates.dropdown(),
            onAccountClicked = {}
        )
    }
}
