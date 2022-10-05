@file:Suppress("TooManyFunctions")

package com.stripe.android.financialconnections.features.accountpicker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
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
import com.stripe.android.financialconnections.exception.NoAccountsAvailableException
import com.stripe.android.financialconnections.exception.NoSupportedPaymentMethodTypeAccountsException
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.PartnerAccountUI
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.SelectionMode
import com.stripe.android.financialconnections.features.common.AccessibleDataCallout
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.NoAccountsAvailableErrorContent
import com.stripe.android.financialconnections.features.common.NoSupportedPaymentMethodTypeAccountsErrorContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun AccountPickerScreen() {
    val viewModel: AccountPickerViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    BackHandler(true) {}
    val state: State<AccountPickerState> = viewModel.collectAsState()
    AccountPickerContent(
        state = state.value,
        onAccountClicked = viewModel::onAccountClicked,
        onSelectAccounts = viewModel::selectAccounts,
        onSelectAnotherBank = viewModel::selectAnotherBank,
        onCloseClick = parentViewModel::onCloseWithConfirmationClick,
        onEnterDetailsManually = viewModel::onEnterDetailsManually,
        onLoadAccountsAgain = viewModel::onLoadAccountsAgain,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
    )
}

@Composable
private fun AccountPickerContent(
    state: AccountPickerState,
    onAccountClicked: (PartnerAccount) -> Unit,
    onSelectAccounts: () -> Unit,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onLoadAccountsAgain: () -> Unit,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                onCloseClick = onCloseClick,
                showBack = false
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> AccountPickerLoading()
            is Success -> when (payload().skipAccountSelection) {
                // ensures account picker is not shown momentarily
                // if account selection should be skipped.
                true -> AccountPickerLoading()
                false -> AccountPickerLoaded(
                    submitEnabled = state.submitEnabled,
                    submitLoading = state.submitLoading,
                    accounts = payload().accounts,
                    selectedIds = state.selectedIds,
                    onAccountClicked = onAccountClicked,
                    onSelectAccounts = onSelectAccounts,
                    selectionMode = payload().selectionMode,
                    accessibleDataCalloutModel = payload().accessibleData
                )
            }

            is Fail -> when (val error = payload.error) {
                is NoSupportedPaymentMethodTypeAccountsException ->
                    NoSupportedPaymentMethodTypeAccountsErrorContent(
                        error,
                        onSelectAnotherBank
                    )
                is NoAccountsAvailableException -> NoAccountsAvailableErrorContent(
                    exception = error,
                    onEnterDetailsManually = onEnterDetailsManually,
                    onTryAgain = onLoadAccountsAgain,
                    onSelectAnotherBank = onSelectAnotherBank
                )
                else -> UnclassifiedErrorContent(
                    error = error,
                    onCloseFromErrorClick = onCloseFromErrorClick
                )
            }
        }
    }
}

@Composable
private fun AccountPickerLoading() {
    LoadingContent(
        stringResource(R.string.stripe_account_picker_loading_title),
        stringResource(R.string.stripe_account_picker_loading_desc)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AccountPickerLoaded(
    submitEnabled: Boolean,
    submitLoading: Boolean,
    accounts: List<PartnerAccountUI>,
    accessibleDataCalloutModel: AccessibleDataCalloutModel?,
    selectionMode: SelectionMode,
    selectedIds: Set<String>,
    onAccountClicked: (PartnerAccount) -> Unit,
    onSelectAccounts: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(R.string.stripe_account_picker_multiselect_account),
                style = FinancialConnectionsTheme.typography.subtitle
            )
            when (selectionMode) {
                SelectionMode.DROPDOWN -> DropdownContent(
                    accounts = accounts,
                    selectedIds = selectedIds,
                    onAccountClicked = onAccountClicked
                )

                SelectionMode.RADIO -> SingleSelectContent(
                    accounts = accounts,
                    selectedIds = selectedIds,
                    onAccountClicked = onAccountClicked
                )

                SelectionMode.CHECKBOXES -> MultiSelectContent(
                    accounts = accounts,
                    selectedIds = selectedIds,
                    onAccountClicked = onAccountClicked
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        accessibleDataCalloutModel?.let { AccessibleDataCallout(it) }
        Spacer(modifier = Modifier.size(12.dp))
        FinancialConnectionsButton(
            enabled = submitEnabled,
            loading = submitLoading,
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

@Suppress("LongMethod")
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DropdownContent(
    accounts: List<PartnerAccountUI>,
    selectedIds: Set<String>,
    onAccountClicked: (PartnerAccount) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOptionText: String? = remember(selectedIds) {
        accounts
            .firstOrNull { selectedIds.contains(it.account.id) }
            ?.let { "${it.account.name} ${it.account.encryptedNumbers}" }
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
            leadingIcon = { if (selectedOptionText != null) InstitutionIcon() },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            }
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
            accounts.forEach { selectedAccount ->
                DropdownMenuItem(
                    onClick = {
                        onAccountClicked(selectedAccount.account)
                        expanded = false
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InstitutionIcon()
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "${selectedAccount.account.name} ${selectedAccount.account.encryptedNumbers}",
                            color = if (selectedAccount.enabled) {
                                FinancialConnectionsTheme.colors.textPrimary
                            } else {
                                FinancialConnectionsTheme.colors.textDisabled
                            },
                            style = FinancialConnectionsTheme.typography.bodyEmphasized
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstitutionIcon() {
    Image(
        painter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
        contentDescription = null,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
    )
}

@Composable
private fun SingleSelectContent(
    accounts: List<PartnerAccountUI>,
    selectedIds: Set<String>,
    onAccountClicked: (PartnerAccount) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(accounts, key = { it.account.id }) { account ->
            AccountItem(
                selected = selectedIds.contains(account.account.id),
                onAccountClicked = onAccountClicked,
                account = account.account,
                enabled = account.enabled
            ) {
                RadioButton(
                    selected = selectedIds.contains(account.account.id),
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
    accounts: List<PartnerAccountUI>,
    selectedIds: Set<String>,
    onAccountClicked: (PartnerAccount) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(accounts, key = { it.account.id }) { account ->
            AccountItem(
                enabled = account.enabled,
                selected = selectedIds.contains(account.account.id),
                onAccountClicked = onAccountClicked,
                account = account.account
            ) {
                Checkbox(
                    checked = selectedIds.contains(account.account.id),
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
    enabled: Boolean,
    selected: Boolean,
    onAccountClicked: (PartnerAccount) -> Unit,
    account: PartnerAccount,
    selectorContent: @Composable () -> Unit
) {
    val padding =
        remember(account) { if (account.displayableAccountNumbers != null) 10.dp else 12.dp }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = when {
                    selected -> FinancialConnectionsTheme.colors.textBrand
                    else -> FinancialConnectionsTheme.colors.borderDefault
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled) { onAccountClicked(account) }
            .padding(padding)
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
                    color = if (enabled) {
                        FinancialConnectionsTheme.colors.textPrimary
                    } else {
                        FinancialConnectionsTheme.colors.textDisabled
                    },
                    style = FinancialConnectionsTheme.typography.bodyEmphasized
                )
                Spacer(modifier = Modifier.size(4.dp))
                account.displayableAccountNumbers?.let {
                    Text(
                        text = "········$it",
                        color = if (enabled) {
                            FinancialConnectionsTheme.colors.textSecondary
                        } else {
                            FinancialConnectionsTheme.colors.textDisabled
                        },
                        style = FinancialConnectionsTheme.typography.body
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
            onAccountClicked = {},
            onSelectAccounts = {},
            onSelectAnotherBank = {},
            onCloseClick = {},
            onEnterDetailsManually = {},
            onLoadAccountsAgain = {},
            onCloseFromErrorClick = {}
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
            onAccountClicked = {},
            onSelectAccounts = {},
            onSelectAnotherBank = {},
            onCloseClick = {},
            onEnterDetailsManually = {},
            onLoadAccountsAgain = {},
            onCloseFromErrorClick = {}
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
            onAccountClicked = {},
            onSelectAccounts = {},
            onSelectAnotherBank = {},
            onCloseClick = {},
            onEnterDetailsManually = {},
            onLoadAccountsAgain = {},
            onCloseFromErrorClick = {}
        )
    }
}
