@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.financialconnections.features.linkaccountpicker

import androidx.activity.compose.BackHandler
import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.AccountItem
import com.stripe.android.financialconnections.features.common.LoadingShimmerEffect
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerState.Payload
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.AddNewAccount
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.clickableSingle
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.LazyLayout
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.utils.collectAsState

/*
  The returning user account picker contains a lot of logic handling what happens after a user selects an account.
  Accounts might require step-up verification, repair, or relinking to grant additional permissions (supportability).
  - For flows where users are only allowed to select one account, the next pane to display is just whatever is set as
    the `next_pane_on_selection` from the API
  - For flows where users may select multiple accounts, we use the following logic:
      - If a selected account requires repair, we immediately pop up a drawer to initiate the repair flow
      - If a selected account requires additional permissions to be shared (supportability),
        we immediately pop up a drawer to initiate partner auth
      - If a selected account requires step-up verification, we assume that all accounts require step-up verification
        and push the step-up verification pane after they click the CTA
      - If a selected account does not require any further action, we continue to the success pane
*/
@Composable
internal fun LinkAccountPickerScreen() {
    val viewModel: LinkAccountPickerViewModel = paneViewModel { LinkAccountPickerViewModel.factory(it) }
    val parentViewModel = parentViewModel()
    val state by viewModel.stateFlow.collectAsState()
    BackHandler(enabled = true) {}

    val uriHandler = LocalUriHandler.current

    state.viewEffect?.let { viewEffect ->
        LaunchedEffect(viewEffect) {
            when (viewEffect) {
                is OpenUrl -> uriHandler.openUri(viewEffect.url)
            }
            viewModel.onViewEffectLaunched()
        }
    }

    LinkAccountPickerContent(
        state = state,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onNewBankAccountClick = viewModel::onNewBankAccountClick,
        onSelectAccountClick = viewModel::onSelectAccountsClick,
        onAccountClick = viewModel::onAccountClick,
    )
}

@Composable
private fun LinkAccountPickerContent(
    state: LinkAccountPickerState,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onClickableTextClick: (String) -> Unit,
    onNewBankAccountClick: () -> Unit,
    onSelectAccountClick: () -> Unit,
    onAccountClick: (PartnerAccount) -> Unit,
) {
    val scrollState = rememberLazyListState()

    Box {
        when (val payload = state.payload) {
            Uninitialized,
            is Loading,
            is Success -> LinkAccountPickerLoaded(
                scrollState = scrollState,
                payload = payload,
                cta = state.cta,
                selectNetworkedAccountAsync = state.selectNetworkedAccountAsync,
                onClickableTextClick = onClickableTextClick,
                onSelectAccountClick = onSelectAccountClick,
                onNewBankAccountClick = onNewBankAccountClick,
                onAccountClick = onAccountClick
            )

            is Fail -> UnclassifiedErrorContent { onCloseFromErrorClick(payload.error) }
        }
    }
}

@Composable
private fun LinkAccountPickerLoaded(
    scrollState: LazyListState,
    payload: Async<Payload>,
    selectNetworkedAccountAsync: Async<Unit>,
    onAccountClick: (PartnerAccount) -> Unit,
    onNewBankAccountClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onSelectAccountClick: () -> Unit,
    cta: TextResource,
) {
    LazyLayout(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        lazyListState = scrollState,
        loading = payload is Loading,
        showPillOnSlowLoad = true,
        body = {
            payload()?.let {
                loadedContent(
                    payload = it,
                    selectNetworkedAccountAsync = selectNetworkedAccountAsync,
                    onAccountClick = onAccountClick,
                    onNewBankAccountClick = onNewBankAccountClick
                )
            } ?: loadingContent()
        },
        footer = {
            payload()?.let {
                Column {
                    it.aboveCta?.let {
                        AnnotatedText(
                            modifier = Modifier.fillMaxWidth(),
                            text = TextResource.Text(fromHtml(it)),
                            onClickableTextClick = onClickableTextClick,
                            defaultStyle = typography.labelSmall.copy(
                                textAlign = TextAlign.Center,
                                color = colors.textDefault
                            )
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                    }
                    FinancialConnectionsButton(
                        enabled = it.selectedAccountIds.isNotEmpty(),
                        loading = selectNetworkedAccountAsync is Loading,
                        onClick = onSelectAccountClick,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(text = cta.toText().toString())
                    }
                }
            }
        }
    )
}

private fun LazyListScope.loadedContent(
    payload: Payload,
    selectNetworkedAccountAsync: Async<Unit>,
    onAccountClick: (PartnerAccount) -> Unit,
    onNewBankAccountClick: () -> Unit
) {
    item {
        AnnotatedText(
            text = TextResource.Text(payload.title),
            defaultStyle = typography.headingXLarge,
            onClickableTextClick = {}
        )
        Spacer(modifier = Modifier.size(8.dp))
    }
    items(payload.accounts) {
        NetworkedAccountItem(
            selected = it.account.id in payload.selectedAccountIds,
            account = it,
            onAccountClicked = { selected ->
                if (selectNetworkedAccountAsync !is Loading) onAccountClick(selected)
            }
        )
    }
    item {
        SelectNewAccount(
            text = payload.addNewAccount,
            onClick = {
                if (selectNetworkedAccountAsync !is Loading) onNewBankAccountClick()
            }
        )
    }
}

private fun LazyListScope.loadingContent() {
    item {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Retrieving accounts",
            style = typography.headingXLarge
        )
        Spacer(modifier = Modifier.size(8.dp))
    }
    items(3) {
        LoadingShimmerEffect {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(it)
            )
        }
    }
}

@Composable
private fun NetworkedAccountItem(
    account: LinkedAccount,
    onAccountClicked: (PartnerAccount) -> Unit,
    selected: Boolean
) {
    val (partnerAccount, networkedAccount) = account
    AccountItem(
        selected = selected,
        onAccountClicked = onAccountClicked,
        account = partnerAccount,
        networkedAccount = networkedAccount
    )
}

@Composable
private fun SelectNewAccount(
    onClick: () -> Unit,
    text: AddNewAccount
) {
    val shape = remember { RoundedCornerShape(16.dp) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = colors.border,
                shape = shape
            )
            .clickableSingle { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectNewAccountIcon(
                icon = text.icon?.default,
                contentDescription = text.body,
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = text.body,
                style = typography.labelLargeEmphasized,
                color = colors.textDefault
            )
        }
    }
}

@Composable
fun SelectNewAccountIcon(
    icon: String?,
    contentDescription: String,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.iconBackground)
    ) {
        val iconModifier = Modifier.size(20.dp)
        val placeholderImage = @Composable {
            Image(
                painter = painterResource(R.drawable.stripe_ic_add),
                modifier = iconModifier,
                colorFilter = ColorFilter.tint(colors.iconBrand),
                contentDescription = contentDescription
            )
        }
        when {
            LocalInspectionMode.current ||
                icon.isNullOrEmpty() -> placeholderImage()

            else -> StripeImage(
                url = icon,
                imageLoader = LocalImageLoader.current,
                contentDescription = null,
                modifier = iconModifier,
                errorContent = { placeholderImage() }
            )
        }
    }
}

//@Composable
//@Preview(group = "LinkAccountPicker Pane")
//internal fun LinkAccountPickerScreenPreview(
//    @PreviewParameter(LinkAccountPickerPreviewParameterProvider::class)
//    state: LinkAccountPickerState
//) {
//    FinancialConnectionsPreview {
//        LinkAccountPickerContent(
//            state = state,
//            onCloseFromErrorClick = {},
//            onClickableTextClick = {},
//            onNewBankAccountClick = {},
//            onSelectAccountClick = {},
//            onAccountClick = {},
//        )
//    }
//}
