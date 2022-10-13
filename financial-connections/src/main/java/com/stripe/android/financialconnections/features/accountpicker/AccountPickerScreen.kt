@file:Suppress("TooManyFunctions", "LongMethod")

package com.stripe.android.financialconnections.features.accountpicker

import androidx.activity.compose.BackHandler
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
import com.stripe.android.financialconnections.features.common.InstitutionPlaceholder
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.NoAccountsAvailableErrorContent
import com.stripe.android.financialconnections.features.common.NoSupportedPaymentMethodTypeAccountsErrorContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.image.StripeImage
import java.text.NumberFormat
import java.util.Currency

@Composable
internal fun AccountPickerScreen() {
    val viewModel: AccountPickerViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    BackHandler(true) {}
    val state: State<AccountPickerState> = viewModel.collectAsState()
    AccountPickerContent(
        state = state.value,
        onAccountClicked = viewModel::onAccountClicked,
        onSubmit = viewModel::onSubmit,
        onSelectAnotherBank = viewModel::selectAnotherBank,
        onCloseClick = parentViewModel::onCloseWithConfirmationClick,
        onEnterDetailsManually = viewModel::onEnterDetailsManually,
        onLoadAccountsAgain = viewModel::onLoadAccountsAgain,
        onSelectAllAccountsClicked = viewModel::onSelectAllAccountsClicked,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
    )
}

@Composable
private fun AccountPickerContent(
    state: AccountPickerState,
    onAccountClicked: (PartnerAccount) -> Unit,
    onSubmit: () -> Unit,
    onSelectAllAccountsClicked: () -> Unit,
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
                    allAccountsSelected = payload().allAccountsSelected,
                    subtitle = payload().subtitle,
                    selectedIds = payload().selectedIds,
                    onAccountClicked = onAccountClicked,
                    onSubmit = onSubmit,
                    selectionMode = payload().selectionMode,
                    accessibleDataCalloutModel = payload().accessibleData,
                    onSelectAllAccountsClicked = onSelectAllAccountsClicked,
                )
            }

            is Fail -> when (val error = payload.error) {
                is NoSupportedPaymentMethodTypeAccountsException ->
                    NoSupportedPaymentMethodTypeAccountsErrorContent(
                        exception = error,
                        onSelectAnotherBank = onSelectAnotherBank,
                        onEnterDetailsManually = onEnterDetailsManually
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
    allAccountsSelected: Boolean,
    accessibleDataCalloutModel: AccessibleDataCalloutModel?,
    selectionMode: SelectionMode,
    selectedIds: Set<String>,
    onAccountClicked: (PartnerAccount) -> Unit,
    onSelectAllAccountsClicked: () -> Unit,
    onSubmit: () -> Unit,
    subtitle: TextResource?
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
            Spacer(modifier = Modifier.size(16.dp))
            subtitle?.let {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = it.toText().toString(),
                    style = FinancialConnectionsTheme.typography.body
                )
            }
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
                    allAccountsSelected = allAccountsSelected,
                    selectedIds = selectedIds,
                    onAccountClicked = onAccountClicked,
                    onSelectAllAccountsClicked = onSelectAllAccountsClicked
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        accessibleDataCalloutModel?.let { AccessibleDataCallout(it) }
        Spacer(modifier = Modifier.size(12.dp))
        FinancialConnectionsButton(
            enabled = submitEnabled,
            loading = submitLoading,
            onClick = onSubmit,
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
    val selectedOption: PartnerAccountUI? = remember(selectedIds) {
        accounts
            .firstOrNull { selectedIds.contains(it.account.id) }
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
            value = selectedOption?.let { "${it.account.name} ${it.account.encryptedNumbers}" }
                ?: stringResource(id = R.string.stripe_account_picker_dropdown_hint),
            onValueChange = { },
            leadingIcon = {
                if (selectedOption != null) InstitutionIcon(selectedOption.institutionIcon)
            },
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
            accounts.forEach { account ->
                AccountDropdownItem(
                    onAccountClicked = {
                        expanded = false
                        onAccountClicked(it)
                    },
                    account = account
                )
            }
        }
    }
}

@Composable
private fun AccountDropdownItem(
    onAccountClicked: (PartnerAccount) -> Unit,
    account: PartnerAccountUI
) {
    DropdownMenuItem(
        onClick = {
            onAccountClicked(account.account)
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InstitutionIcon(account.institutionIcon)
            Spacer(modifier = Modifier.size(8.dp))
            val (title, subtitle) = getAccountTexts(
                account = account.account,
                enabled = account.enabled
            )
            Column {
                Text(
                    text = title,
                    color = if (account.enabled) {
                        FinancialConnectionsTheme.colors.textPrimary
                    } else {
                        FinancialConnectionsTheme.colors.textDisabled
                    },
                    style = FinancialConnectionsTheme.typography.body
                )
                subtitle?.let {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = it,
                        color = FinancialConnectionsTheme.colors.textDisabled,
                        style = FinancialConnectionsTheme.typography.caption
                    )
                }
            }
        }
    }
}

@Composable
private fun InstitutionIcon(institutionIcon: String?) {
    val modifier = Modifier
        .size(36.dp)
        .clip(RoundedCornerShape(6.dp))
    StripeImage(
        url = institutionIcon ?: "",
        errorContent = { InstitutionPlaceholder(modifier) },
        imageLoader = LocalImageLoader.current,
        contentDescription = null,
        modifier = modifier
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
    onAccountClicked: (PartnerAccount) -> Unit,
    onSelectAllAccountsClicked: () -> Unit,
    allAccountsSelected: Boolean
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item("select_all_accounts") {
            AccountItem(
                enabled = true,
                selected = allAccountsSelected,
                onAccountClicked = { onSelectAllAccountsClicked() },
                account = PartnerAccount(
                    id = "select_all_accounts",
                    authorization = "",
                    category = FinancialConnectionsAccount.Category.UNKNOWN,
                    subcategory = FinancialConnectionsAccount.Subcategory.UNKNOWN,
                    name = stringResource(R.string.stripe_account_picker_select_all_accounts),
                    supportedPaymentMethodTypes = emptyList()
                ),
            ) {
                Checkbox(
                    checked = allAccountsSelected,
                    colors = CheckboxDefaults.colors(
                        checkedColor = FinancialConnectionsTheme.colors.textBrand,
                        checkmarkColor = FinancialConnectionsTheme.colors.textWhite,
                        uncheckedColor = FinancialConnectionsTheme.colors.borderDefault
                    ),
                    onCheckedChange = null
                )
            }
        }
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
            val (title, subtitle) = getAccountTexts(
                account = account,
                enabled = enabled
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(
                    text = title,
                    color = if (enabled) {
                        FinancialConnectionsTheme.colors.textPrimary
                    } else {
                        FinancialConnectionsTheme.colors.textDisabled
                    },
                    style = FinancialConnectionsTheme.typography.bodyEmphasized
                )
                subtitle?.let {
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = it,
                        color = FinancialConnectionsTheme.colors.textDisabled,
                        style = FinancialConnectionsTheme.typography.body
                    )
                }
            }
        }
    }
}

@Composable
private fun getAccountTexts(
    account: PartnerAccount,
    enabled: Boolean
): Pair<String, String?> {
    val balance = if (account.balanceAmount != null && account.currency != null) {
        NumberFormat
            .getCurrencyInstance()
            .also { it.currency = Currency.getInstance(account.currency) }
            .format(account.balanceAmount)
    } else null
    val title = when {
        balance != null -> "${account.name} ${account.encryptedNumbers}"
        else -> account.name
    }
    val subtitle = when {
        enabled.not() -> stringResource(id = R.string.stripe_account_picker_must_be_bank_account)
        balance != null -> balance
        account.encryptedNumbers.isNotEmpty() -> account.encryptedNumbers
        else -> null
    }
    return title to subtitle
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
            onSubmit = {},
            onSelectAnotherBank = {},
            onCloseClick = {},
            onEnterDetailsManually = {},
            onLoadAccountsAgain = {},
            onCloseFromErrorClick = {},
            onSelectAllAccountsClicked = {}
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
            onSubmit = {},
            onSelectAnotherBank = {},
            onCloseClick = {},
            onEnterDetailsManually = {},
            onLoadAccountsAgain = {},
            onCloseFromErrorClick = {},
            onSelectAllAccountsClicked = {}
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
            onSubmit = {},
            onSelectAnotherBank = {},
            onCloseClick = {},
            onEnterDetailsManually = {},
            onLoadAccountsAgain = {},
            onCloseFromErrorClick = {},
            onSelectAllAccountsClicked = {}
        )
    }
}
