@file:Suppress("TooManyFunctions", "LongMethod")

package com.stripe.android.financialconnections.features.accountpicker

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.exception.AccountLoadError
import com.stripe.android.financialconnections.exception.AccountNoneEligibleForPaymentMethodError
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.PartnerAccountUI
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.SelectionMode
import com.stripe.android.financialconnections.features.common.AccessibleDataCallout
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.NoAccountsAvailableErrorContent
import com.stripe.android.financialconnections.features.common.NoSupportedPaymentMethodTypeAccountsErrorContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.uicore.text.MiddleEllipsisText

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
        onSelectAllAccountsClicked = viewModel::onSelectAllAccountsClicked,
        onSelectAnotherBank = viewModel::selectAnotherBank,
        onEnterDetailsManually = viewModel::onEnterDetailsManually,
        onLoadAccountsAgain = viewModel::onLoadAccountsAgain,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.ACCOUNT_PICKER) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onLearnMoreAboutDataAccessClick = viewModel::onLearnMoreAboutDataAccessClick
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
    onLearnMoreAboutDataAccessClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = false,
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> AccountPickerLoading()
            is Success -> when (payload().shouldSkipPane) {
                // ensures account picker is not shown momentarily
                // if account selection should be skipped.
                true -> AccountPickerLoading()
                false -> AccountPickerLoaded(
                    submitEnabled = state.submitEnabled,
                    submitLoading = state.submitLoading,
                    accounts = payload().accounts,
                    allAccountsSelected = state.allAccountsSelected,
                    subtitle = payload().subtitle,
                    selectedIds = state.selectedIds,
                    onAccountClicked = onAccountClicked,
                    onSubmit = onSubmit,
                    selectionMode = payload().selectionMode,
                    accessibleDataCalloutModel = payload().accessibleData,
                    requiresSingleAccountConfirmation = payload().requiresSingleAccountConfirmation,
                    onSelectAllAccountsClicked = onSelectAllAccountsClicked,
                    onLearnMoreAboutDataAccessClick = onLearnMoreAboutDataAccessClick
                )
            }

            is Fail -> when (val error = payload.error) {
                is AccountNoneEligibleForPaymentMethodError ->
                    NoSupportedPaymentMethodTypeAccountsErrorContent(
                        exception = error,
                        onSelectAnotherBank = onSelectAnotherBank,
                        onEnterDetailsManually = onEnterDetailsManually
                    )

                is AccountLoadError -> NoAccountsAvailableErrorContent(
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
        title = stringResource(R.string.stripe_account_picker_loading_title),
        content = stringResource(R.string.stripe_account_picker_loading_desc)
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
    requiresSingleAccountConfirmation: Boolean,
    selectionMode: SelectionMode,
    selectedIds: Set<String>,
    onAccountClicked: (PartnerAccount) -> Unit,
    onSelectAllAccountsClicked: () -> Unit,
    onSubmit: () -> Unit,
    onLearnMoreAboutDataAccessClick: () -> Unit,
    subtitle: TextResource?
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(
                top = 16.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp
            )
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(
                    when (requiresSingleAccountConfirmation) {
                        true -> R.string.stripe_account_picker_confirm_account
                        false -> when (selectionMode) {
                            SelectionMode.RADIO -> R.string.stripe_account_picker_singleselect_account
                            SelectionMode.CHECKBOXES -> R.string.stripe_account_picker_multiselect_account
                        }
                    }
                ),
                style = FinancialConnectionsTheme.typography.subtitle
            )
            subtitle?.let {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = it.toText().toString(),
                    style = FinancialConnectionsTheme.typography.body
                )
            }
            Spacer(modifier = Modifier.size(24.dp))
            when (selectionMode) {
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
        accessibleDataCalloutModel?.let {
            AccessibleDataCallout(
                it,
                onLearnMoreAboutDataAccessClick
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        FinancialConnectionsButton(
            enabled = submitEnabled,
            loading = submitLoading,
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = when (requiresSingleAccountConfirmation) {
                    true -> stringResource(R.string.stripe_account_picker_cta_confirm)
                    false -> pluralStringResource(
                        count = selectedIds.size,
                        id = R.plurals.stripe_account_picker_cta_link
                    )
                }
            )
        }
    }
}

@Composable
private fun SingleSelectContent(
    accounts: List<PartnerAccountUI>,
    selectedIds: Set<String>,
    onAccountClicked: (PartnerAccount) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(accounts, key = { it.account.id }) { account ->
            AccountItem(
                selected = selectedIds.contains(account.account.id),
                onAccountClicked = onAccountClicked,
                accountUI = account,
                selectorContent = {
                    FinancialConnectionRadioButton(
                        checked = selectedIds.contains(account.account.id),
                    )
                },
            )
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
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item("select_all_accounts") {
            AccountItem(
                selected = allAccountsSelected,
                onAccountClicked = { onSelectAllAccountsClicked() },
                accountUI = PartnerAccountUI(
                    PartnerAccount(
                        id = "select_all_accounts",
                        _allowSelection = true,
                        allowSelectionMessage = "",
                        authorization = "",
                        category = FinancialConnectionsAccount.Category.UNKNOWN,
                        subcategory = FinancialConnectionsAccount.Subcategory.UNKNOWN,
                        name = stringResource(R.string.stripe_account_picker_select_all_accounts),
                        supportedPaymentMethodTypes = emptyList()
                    ),
                    formattedBalance = null,
                    institutionIcon = null
                )
            ) {
                FinancialConnectionCheckbox(
                    allAccountsSelected,
                )
            }
        }
        items(accounts, key = { it.account.id }) { account ->
            AccountItem(
                selected = selectedIds.contains(account.account.id),
                onAccountClicked = onAccountClicked,
                accountUI = account
            ) {
                FinancialConnectionCheckbox(
                    checked = selectedIds.contains(account.account.id),
                )
            }
        }
    }
}

@Composable
private fun FinancialConnectionCheckbox(
    checked: Boolean,
) {
    Crossfade(targetState = checked) {
        Image(
            painter = painterResource(
                if (it) {
                    R.drawable.stripe_ic_checkbox_yes
                } else {
                    R.drawable.stripe_ic_checkbox_no
                },
            ),
            contentDescription = null,
        )
    }
}

@Composable
private fun FinancialConnectionRadioButton(
    checked: Boolean,
) {
    Crossfade(targetState = checked) {
        Image(
            painter = painterResource(
                if (it) {
                    R.drawable.stripe_ic_radio_yes
                } else {
                    R.drawable.stripe_ic_radio_no
                },
            ),
            contentDescription = null,
        )
    }
}

@Composable
@Suppress("MagicNumber")
private fun AccountItem(
    selected: Boolean,
    onAccountClicked: (PartnerAccount) -> Unit,
    accountUI: PartnerAccountUI,
    selectorContent: @Composable RowScope.() -> Unit
) {
    val account = accountUI.account
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
                    selected -> colors.textBrand
                    else -> colors.borderDefault
                },
                shape = shape
            )
            .clickable(enabled = accountUI.account.allowSelection) { onAccountClicked(account) }
            .padding(vertical = verticalPadding, horizontal = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            selectorContent()
            Spacer(modifier = Modifier.size(16.dp))
            val (title, subtitle) = getAccountTexts(accountUI = accountUI)
            Column(
                Modifier.weight(0.7f)
            ) {
                MiddleEllipsisText(
                    text = title,
                    color = if (accountUI.account.allowSelection) {
                        colors.textPrimary
                    } else {
                        colors.textDisabled
                    },
                    style = FinancialConnectionsTheme.typography.bodyEmphasized
                )
                subtitle?.let {
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = it,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        color = colors.textDisabled,
                        style = FinancialConnectionsTheme.typography.captionTight
                    )
                }
            }
        }
    }
}

@Composable
private fun getAccountTexts(
    accountUI: PartnerAccountUI,
): Pair<String, String?> {
    val account = accountUI.account
    val title = when {
        account.allowSelection.not() ||
            accountUI.formattedBalance != null -> "${account.name} ${account.encryptedNumbers}"

        else -> account.name
    }
    val subtitle = when {
        account.allowSelection.not() -> account.allowSelectionMessage
        accountUI.formattedBalance != null -> accountUI.formattedBalance
        account.encryptedNumbers.isNotEmpty() -> account.encryptedNumbers
        else -> null
    }
    return title to subtitle
}

@Preview(
    showBackground = true,
    group = "Account Picker Pane",
)
@Composable
internal fun AccountPickerPreview(
    @PreviewParameter(AccountPickerPreviewParameterProvider::class) state: AccountPickerState
) {
    FinancialConnectionsPreview {
        AccountPickerContent(
            state = state,
            onAccountClicked = {},
            onSubmit = {},
            onSelectAllAccountsClicked = {},
            onSelectAnotherBank = {},
            onEnterDetailsManually = {},
            onLoadAccountsAgain = {},
            onCloseClick = {},
            onLearnMoreAboutDataAccessClick = {}
        ) {}
    }
}
