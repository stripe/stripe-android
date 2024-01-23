@file:Suppress("TooManyFunctions", "LongMethod")

package com.stripe.android.financialconnections.features.accountpicker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.exception.AccountLoadError
import com.stripe.android.financialconnections.exception.AccountNoneEligibleForPaymentMethodError
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerClickableText.DATA
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.SelectionMode
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.ViewEffect.OpenBottomSheet
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.common.AccountItem
import com.stripe.android.financialconnections.features.common.DataAccessBottomSheetContent
import com.stripe.android.financialconnections.features.common.LoadingShimmerEffect
import com.stripe.android.financialconnections.features.common.MerchantDataAccessModel
import com.stripe.android.financialconnections.features.common.MerchantDataAccessText
import com.stripe.android.financialconnections.features.common.NoAccountsAvailableErrorContent
import com.stripe.android.financialconnections.features.common.NoSupportedPaymentMethodTypeAccountsErrorContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Layout
import com.stripe.android.financialconnections.ui.theme.Neutral900
import kotlinx.coroutines.launch

@Composable
internal fun AccountPickerScreen() {
    val viewModel: AccountPickerViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state: State<AccountPickerState> = viewModel.collectAsState()
    BackHandler(true) {}

    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val uriHandler = LocalUriHandler.current

    state.value.viewEffect?.let { viewEffect ->
        LaunchedEffect(viewEffect) {
            when (viewEffect) {
                is OpenUrl -> uriHandler.openUri(viewEffect.url)
                is OpenBottomSheet -> bottomSheetState.show()
            }
            viewModel.onViewEffectLaunched()
        }
    }

    AccountPickerContent(
        state = state.value,
        bottomSheetState = bottomSheetState,
        onAccountClicked = viewModel::onAccountClicked,
        onSubmit = viewModel::onSubmit,
        onSelectAnotherBank = viewModel::selectAnotherBank,
        onEnterDetailsManually = viewModel::onEnterDetailsManually,
        onLoadAccountsAgain = viewModel::onLoadAccountsAgain,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.ACCOUNT_PICKER) },
        onClickableTextClick = viewModel::onClickableTextClick,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun AccountPickerContent(
    state: AccountPickerState,
    bottomSheetState: ModalBottomSheetState,
    onAccountClicked: (PartnerAccount) -> Unit,
    onClickableTextClick: (String) -> Unit,
    onSubmit: () -> Unit,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onLoadAccountsAgain: () -> Unit,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetBackgroundColor = FinancialConnectionsTheme.v3Colors.backgroundSurface,
        sheetShape = RoundedCornerShape(8.dp),
        scrimColor = Neutral900.copy(alpha = 0.32f),
        sheetContent = {
            when (val dataAccessNotice = state.payload()?.dataAccessNotice) {
                null -> Unit
                else -> DataAccessBottomSheetContent(
                    dataDialog = dataAccessNotice,
                    onConfirmModalClick = { scope.launch { bottomSheetState.hide() } },
                    onClickableTextClick = onClickableTextClick
                )
            }
        },
    ) {
        FinancialConnectionsScaffold(
            topBar = {
                FinancialConnectionsTopAppBar(
                    showBack = false,
                    onCloseClick = onCloseClick,
                    elevation = lazyListState.elevation
                )
            }
        ) {
            when (val payload = state.payload) {
                is Fail -> {
                    when (val error = payload.error) {
                        is AccountNoneEligibleForPaymentMethodError ->
                            NoSupportedPaymentMethodTypeAccountsErrorContent(
                                exception = error,
                                onSelectAnotherBank = onSelectAnotherBank
                            )

                        is AccountLoadError -> NoAccountsAvailableErrorContent(
                            exception = error,
                            onEnterDetailsManually = onEnterDetailsManually,
                            onTryAgain = onLoadAccountsAgain,
                            onSelectAnotherBank = onSelectAnotherBank
                        )

                        else -> UnclassifiedErrorContent(
                            error,
                            onCloseFromErrorClick = onCloseFromErrorClick
                        )
                    }
                }

                is Loading,
                is Uninitialized,
                is Success -> AccountPickerMainContent(
                    payload = payload,
                    state = state,
                    onAccountClicked = onAccountClicked,
                    onClickableTextClick = onClickableTextClick,
                    lazyListState = lazyListState,
                    onSubmit = onSubmit
                )
            }
        }
    }
}

@Composable
private fun AccountPickerMainContent(
    payload: Async<AccountPickerState.Payload>,
    state: AccountPickerState,
    lazyListState: LazyListState,
    onAccountClicked: (PartnerAccount) -> Unit,
    onClickableTextClick: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Layout(
        lazyListState = lazyListState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        body = {
            payload()
                ?.takeIf { it.shouldSkipPane.not() }
                ?.let {
                    loadedContent(
                        payload = it,
                        state = state,
                        onAccountClicked = onAccountClicked
                    )
                } ?: run { loadingContent() }
        },
        footer = {
            payload()
                ?.takeIf { it.shouldSkipPane.not() }
                ?.let {
                    Footer(
                        merchantDataAccessModel = it.merchantDataAccess,
                        onClickableTextClick = onClickableTextClick,
                        submitEnabled = state.submitEnabled,
                        submitLoading = state.submitLoading,
                        onSubmit = onSubmit,
                        selectedIds = state.selectedIds
                    )
                }
        }

    )
}

private fun LazyListScope.loadedContent(
    payload: AccountPickerState.Payload,
    state: AccountPickerState,
    onAccountClicked: (PartnerAccount) -> Unit
) {
    item {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(
                when (payload.selectionMode) {
                    SelectionMode.Single -> R.string.stripe_account_picker_singleselect_account
                    SelectionMode.Multiple -> R.string.stripe_account_picker_multiselect_account
                }
            ),
            style = FinancialConnectionsTheme.v3Typography.headingXLarge
        )
    }
    items(payload.accounts, key = { it.id }) { account ->
        AccountItem(
            selected = state.selectedIds.contains(account.id),
            onAccountClicked = onAccountClicked,
            account = account,
        )
    }
}

@Suppress("MagicNumber")
private fun LazyListScope.loadingContent() {
    item {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Retrieving accounts",
            style = FinancialConnectionsTheme.v3Typography.headingXLarge
        )
    }
    items(3) {
        LoadingShimmerEffect {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(it)
            )
        }
    }
}

@Composable
private fun Footer(
    merchantDataAccessModel: MerchantDataAccessModel?,
    onClickableTextClick: (String) -> Unit,
    submitEnabled: Boolean,
    submitLoading: Boolean,
    onSubmit: () -> Unit,
    selectedIds: Set<String>
) {
    Column {
        merchantDataAccessModel?.let {
            MerchantDataAccessText(
                model = it,
                onLearnMoreClick = { onClickableTextClick(DATA.value) }
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
                text = pluralStringResource(
                    count = selectedIds.size,
                    id = R.plurals.stripe_account_picker_cta_link
                )

            )
        }
    }
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
            onSelectAnotherBank = {},
            onEnterDetailsManually = {},
            onLoadAccountsAgain = {},
            onCloseClick = {},
            onCloseFromErrorClick = {},
            onClickableTextClick = {},
            bottomSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true
            ),
        )
    }
}
