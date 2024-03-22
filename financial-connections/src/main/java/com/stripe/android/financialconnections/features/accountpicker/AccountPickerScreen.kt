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
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerClickableText.DATA
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.SelectionMode
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.ViewEffect.OpenBottomSheet
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerViewAction.OnAccountClicked
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerViewAction.OnClickableTextClicked
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerViewAction.OnSubmitClicked
import com.stripe.android.financialconnections.features.common.AccountItem
import com.stripe.android.financialconnections.features.common.DataAccessBottomSheetContent
import com.stripe.android.financialconnections.features.common.LoadingShimmerEffect
import com.stripe.android.financialconnections.features.common.MerchantDataAccessModel
import com.stripe.android.financialconnections.features.common.MerchantDataAccessText
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.presentation.FinancialConnectionsErrorAction
import com.stripe.android.financialconnections.presentation.FinancialConnectionsErrorAction.Close
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.LazyLayout
import com.stripe.android.financialconnections.ui.theme.LoadableContent
import com.stripe.android.financialconnections.ui.theme.LoadableContentCustomMapping
import com.stripe.android.financialconnections.ui.theme.Neutral900
import kotlinx.coroutines.launch

@Composable
internal fun AccountPickerScreen() {
    val viewModel: AccountPickerViewModel = mavericksViewModel()
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
        onViewAction = viewModel::handleViewAction,
        onErrorAction = viewModel::handleErrorAction,
    )
}

@Composable
private fun AccountPickerContent(
    state: AccountPickerState,
    bottomSheetState: ModalBottomSheetState,
    onViewAction: (AccountPickerViewAction) -> Unit,
    onErrorAction: (FinancialConnectionsErrorAction) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetBackgroundColor = FinancialConnectionsTheme.colors.backgroundSurface,
        sheetShape = RoundedCornerShape(8.dp),
        scrimColor = Neutral900.copy(alpha = 0.32f),
        sheetContent = {
            when (val dataAccessNotice = state.payload()?.dataAccessNotice) {
                null -> Unit
                else -> DataAccessBottomSheetContent(
                    dataDialog = dataAccessNotice,
                    onConfirmModalClick = { scope.launch { bottomSheetState.hide() } },
                    onClickableTextClick = { onViewAction(OnClickableTextClicked(it)) }
                )
            }
        },
        content = {
            AccountPickerMainContent(
                lazyListState = lazyListState,
                state = state,
                onViewAction = onViewAction,
                onErrorAction = onErrorAction,
                onCloseFromErrorClick = { onErrorAction(Close(it)) },
            )
        },
    )
}

@Composable
private fun AccountPickerMainContent(
    lazyListState: LazyListState,
    state: AccountPickerState,
    onViewAction: (AccountPickerViewAction) -> Unit,
    onErrorAction: (FinancialConnectionsErrorAction) -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
) {
    LoadableContent(
        payload = state.payload,
        onErrorAction = onErrorAction,
        customMapping = LoadableContentCustomMapping(
            showContentOnIncomplete = true,
        ),
    ) {
        AccountPickerLoaded(
            payload = state.payload,
            state = state,
            onAccountClicked = { onViewAction(OnAccountClicked(it)) },
            onClickableTextClick = { onViewAction(OnClickableTextClicked(it)) },
            lazyListState = lazyListState,
            onSubmit = { onViewAction(OnSubmitClicked) },
        )
    }
}

@Composable
private fun AccountPickerLoaded(
    payload: Async<AccountPickerState.Payload>,
    state: AccountPickerState,
    lazyListState: LazyListState,
    onAccountClicked: (PartnerAccount) -> Unit,
    onClickableTextClick: (String) -> Unit,
    onSubmit: () -> Unit
) {
    LazyLayout(
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
            style = FinancialConnectionsTheme.typography.headingXLarge
        )
    }
    items(payload.accounts, key = { it.id }) { account ->
        AccountItem(
            selected = state.selectedIds.contains(account.id),
            showInstitutionIcon = false,
            onAccountClicked = onAccountClicked,
            account = account,
        )
    }
}

private fun LazyListScope.loadingContent() {
    item {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Retrieving accounts",
            style = FinancialConnectionsTheme.typography.headingXLarge
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
            onViewAction = {},
            onErrorAction = {},
            bottomSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true
            ),
        )
    }
}
