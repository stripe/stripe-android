package com.stripe.android.financialconnections.features.accountpicker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.exception.AccountLoadError
import com.stripe.android.financialconnections.exception.AccountNoneEligibleForPaymentMethodError
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.SelectionMode
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.common.AccountItem
import com.stripe.android.financialconnections.features.common.InstitutionIcon
import com.stripe.android.financialconnections.features.common.LoadingShimmerEffect
import com.stripe.android.financialconnections.features.common.NoAccountsAvailableErrorContent
import com.stripe.android.financialconnections.features.common.NoSupportedPaymentMethodTypeAccountsErrorContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.LazyLayout
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun AccountPickerScreen() {
    val viewModel: AccountPickerViewModel = paneViewModel { AccountPickerViewModel.factory(it) }
    val parentViewModel = parentViewModel()
    val state: State<AccountPickerState> = viewModel.stateFlow.collectAsState()
    BackHandler(true) {}

    val uriHandler = LocalUriHandler.current

    state.value.viewEffect?.let { viewEffect ->
        LaunchedEffect(viewEffect) {
            when (viewEffect) {
                is OpenUrl -> uriHandler.openUri(viewEffect.url)
            }
            viewModel.onViewEffectLaunched()
        }
    }

    AccountPickerContent(
        state = state.value,
        onAccountClicked = viewModel::onAccountClicked,
        onSubmit = viewModel::onSubmit,
        onSelectAnotherBank = viewModel::selectAnotherBank,
        onEnterDetailsManually = viewModel::onEnterDetailsManually,
        onLoadAccountsAgain = viewModel::onLoadAccountsAgain,
        onClickableTextClick = viewModel::onClickableTextClick,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun AccountPickerContent(
    state: AccountPickerState,
    onAccountClicked: (PartnerAccount) -> Unit,
    onClickableTextClick: (String) -> Unit,
    onSubmit: () -> Unit,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onLoadAccountsAgain: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    val lazyListState = rememberLazyListState()

    Box {
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

                    else -> UnclassifiedErrorContent { onCloseFromErrorClick(error) }
                }
            }

            is Loading,
            is Uninitialized,
            is Success -> AccountPickerLoaded(
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

@Composable
private fun AccountPickerLoaded(
    payload: Async<AccountPickerState.Payload>,
    state: AccountPickerState,
    lazyListState: LazyListState,
    onAccountClicked: (PartnerAccount) -> Unit,
    onClickableTextClick: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val displayablePayload = payload()?.takeIf { it.shouldSkipPane.not() }

    LazyLayout(
        lazyListState = lazyListState,
        bodyPadding = PaddingValues(
            top = 0.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        loading = payload is Loading,
        showPillOnSlowLoad = true,
        body = {
            accountPickerContent(
                institution = state.institution(),
                payload = displayablePayload,
                selectedIds = state.selectedIds,
                onAccountClicked = onAccountClicked,
            )
        },
        footer = {
            displayablePayload?.let {
                Footer(
                    dataAccessDisclaimer = it.dataAccessDisclaimer,
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

private fun LazyListScope.accountPickerContent(
    institution: FinancialConnectionsInstitution?,
    payload: AccountPickerState.Payload?,
    selectedIds: Set<String>,
    onAccountClicked: (PartnerAccount) -> Unit,
) {
    item("icon") {
        InstitutionIcon(
            institutionIcon = institution?.icon?.default,
            modifier = Modifier.padding(top = 16.dp),
            disablePlaceholder = true,
        )
    }

    item("header") {
        if (payload != null) {
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
        } else {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Retrieving accounts",
                style = FinancialConnectionsTheme.typography.headingXLarge
            )
        }
    }

    if (payload != null) {
        items(payload.accounts, key = { it.id }) { account ->
            AccountItem(
                selected = selectedIds.contains(account.id),
                showInstitutionIcon = false,
                onAccountClicked = onAccountClicked,
                account = account,
            )
        }
    } else {
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
}

@Composable
private fun Footer(
    dataAccessDisclaimer: String?,
    onClickableTextClick: (String) -> Unit,
    submitEnabled: Boolean,
    submitLoading: Boolean,
    onSubmit: () -> Unit,
    selectedIds: Set<String>
) {
    Column {
        if (dataAccessDisclaimer != null) {
            DataAccessDisclaimerText(
                text = dataAccessDisclaimer,
                onLearnMoreClick = onClickableTextClick,
            )
            Spacer(modifier = Modifier.size(12.dp))
        }
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

@Composable
private fun DataAccessDisclaimerText(
    text: String,
    onLearnMoreClick: (String) -> Unit
) {
    AnnotatedText(
        modifier = Modifier.fillMaxWidth(),
        text = TextResource.Text(fromHtml(text)),
        onClickableTextClick = onLearnMoreClick,
        defaultStyle = FinancialConnectionsTheme.typography.labelSmall.copy(
            color = FinancialConnectionsTheme.colors.textDefault,
            textAlign = TextAlign.Center,
        ),
    )
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
            onCloseFromErrorClick = {},
            onClickableTextClick = {},
        )
    }
}
