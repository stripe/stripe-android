@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.financialconnections.features.linkaccountpicker

import androidx.activity.compose.BackHandler
import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.AccessibleDataCallout
import com.stripe.android.financialconnections.features.common.AccountItem
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun LinkAccountPickerScreen() {
    val viewModel: LinkAccountPickerViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    LinkAccountPickerContent(
        state = state.value,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.LINK_ACCOUNT_PICKER) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onLearnMoreAboutDataAccessClick = viewModel::onLearnMoreAboutDataAccessClick,
        onNewBankAccountClick = viewModel::onNewBankAccountClick,
        onSelectAccountClick = viewModel::onSelectAccountClick,
        onAccountClick = viewModel::onAccountClick
    )
}

@Composable
private fun LinkAccountPickerContent(
    state: LinkAccountPickerState,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onLearnMoreAboutDataAccessClick: () -> Unit,
    onNewBankAccountClick: () -> Unit,
    onSelectAccountClick: () -> Unit,
    onAccountClick: (PartnerAccount) -> Unit
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
            Uninitialized, is Loading -> LoadingContent()
            is Success -> LinkAccountPickerLoaded(
                payload = payload(),
                selectedAccountId = state.selectedAccountId,
                selectNetworkedAccountAsync = state.selectNetworkedAccountAsync,
                onLearnMoreAboutDataAccessClick = onLearnMoreAboutDataAccessClick,
                onSelectAccountClick = onSelectAccountClick,
                onNewBankAccountClick = onNewBankAccountClick,
                onAccountClick = onAccountClick
            )

            is Fail -> UnclassifiedErrorContent(
                error = payload.error,
                onCloseFromErrorClick = onCloseFromErrorClick
            )
        }
    }
}

@Composable
private fun LinkAccountPickerLoaded(
    selectedAccountId: String?,
    selectNetworkedAccountAsync: Async<Unit>,
    payload: Payload,
    onLearnMoreAboutDataAccessClick: () -> Unit,
    onSelectAccountClick: () -> Unit,
    onNewBankAccountClick: () -> Unit,
    onAccountClick: (PartnerAccount) -> Unit
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
            Spacer(modifier = Modifier.size(16.dp))
            Title(merchantName = payload.businessName)
            Spacer(modifier = Modifier.size(24.dp))
            payload.accounts.forEach {
                NetworkedAccountItem(
                    selected = it.id == selectedAccountId,
                    account = it,
                    onAccountClicked = { selected ->
                        if (selectNetworkedAccountAsync !is Loading) onAccountClick(selected)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            SelectNewAccount(onClick = onNewBankAccountClick)
        }
        AccessibleDataCallout(
            payload.accessibleData,
            onLearnMoreAboutDataAccessClick
        )
        Spacer(modifier = Modifier.size(12.dp))
        FinancialConnectionsButton(
            enabled = selectedAccountId != null,
            loading = selectNetworkedAccountAsync is Loading,
            onClick = onSelectAccountClick,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.stripe_link_account_picker_cta))
        }
    }
}

@Composable
private fun NetworkedAccountItem(
    account: PartnerAccount,
    onAccountClicked: (PartnerAccount) -> Unit,
    selected: Boolean
) {
    AccountItem(
        selected = selected,
        onAccountClicked = onAccountClicked,
        account = account
    ) {
        Image(
            painter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
            contentDescription = "Bank logo"
        )
    }
}


@Composable
private fun SelectNewAccount(
    onClick: () -> Unit
) {
    val shape = remember { RoundedCornerShape(8.dp) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = FinancialConnectionsTheme.colors.borderDefault,
                shape = shape
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.stripe_link_account_picker_new_account)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(id = R.string.stripe_link_account_picker_new_account),
                style = FinancialConnectionsTheme.typography.body,
                color = FinancialConnectionsTheme.colors.textBrand
            )
        }
    }
}

@Composable
private fun Title(
    merchantName: String
) {
    AnnotatedText(
        text = TextResource.Text(
            stringResource(
                R.string.stripe_link_account_picker_title,
                merchantName
            )
        ),
        defaultStyle = FinancialConnectionsTheme.typography.subtitle,
        annotationStyles = emptyMap(),
        onClickableTextClick = {},
    )
}

@Composable
@Preview(group = "LinkAccountPicker Pane", name = "Canonical")
internal fun LinkAccountPickerScreenPreview() {
    FinancialConnectionsPreview {
        LinkAccountPickerContent(
            state = LinkedAccountPickerStates.canonical(),
            onCloseClick = {},
            onCloseFromErrorClick = {},
            onLearnMoreAboutDataAccessClick = {},
            onNewBankAccountClick = {},
            onSelectAccountClick = {},
            onAccountClick = {}
        )
    }
}
