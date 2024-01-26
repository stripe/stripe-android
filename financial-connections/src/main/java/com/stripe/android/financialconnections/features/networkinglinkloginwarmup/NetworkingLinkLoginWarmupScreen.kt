@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
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
import com.stripe.android.financialconnections.features.common.CircleIcon
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupState.Payload
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupViewModel.Companion.PANE
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import com.stripe.android.financialconnections.ui.theme.Layout
import com.stripe.android.financialconnections.ui.theme.LinkColors

@Composable
internal fun NetworkingLinkLoginWarmupScreen() {
    val viewModel: NetworkingLinkLoginWarmupViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    NetworkingLinkLoginWarmupContent(
        state = state.value,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(PANE) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onSkipClicked = viewModel::onSkipClicked,
        onContinueClick = viewModel::onContinueClick,
    )
}

@Composable
private fun NetworkingLinkLoginWarmupContent(
    state: NetworkingLinkLoginWarmupState,
    onCloseClick: () -> Unit,
    onContinueClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onSkipClicked: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = true,
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized,
            is Loading -> FullScreenGenericLoading()

            is Fail -> UnclassifiedErrorContent(
                error = payload.error,
                onCloseFromErrorClick = onCloseFromErrorClick
            )

            is Success -> NetworkingLinkLoginWarmupLoaded(
                lazyListState = lazyListState,
                payload = payload(),
                disableNetworkingAsync = state.disableNetworkingAsync,
                onSkipClicked = onSkipClicked,
                onCloseFromErrorClick = onCloseFromErrorClick,
                onContinueClick = onContinueClick
            )
        }
    }
}

@Composable
private fun NetworkingLinkLoginWarmupLoaded(
    lazyListState: LazyListState,
    payload: Payload,
    disableNetworkingAsync: Async<FinancialConnectionsSessionManifest>,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onSkipClicked: () -> Unit,
    onContinueClick: () -> Unit,
) {
    if (disableNetworkingAsync is Fail) {
        UnclassifiedErrorContent(
            error = disableNetworkingAsync.error,
            onCloseFromErrorClick = onCloseFromErrorClick
        )
    } else {
        Layout(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            lazyListState = lazyListState,
            body = {
                item { HeaderSection() }
                item { ExistingEmailSection(email = payload.email) }
            },
            footer = {
                Footer(
                    disableNetworkingAsync = disableNetworkingAsync,
                    onContinueClick = onContinueClick,
                    onSkipClicked = onSkipClicked
                )
            }
        )
    }
}

@Composable
private fun HeaderSection(
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircleIcon(
            painter = painterResource(id = R.drawable.stripe_ic_person),
            contentDescription = stringResource(R.string.stripe_networking_link_login_warmup_title)
        )
        Text(
            text = stringResource(R.string.stripe_networking_link_login_warmup_title),
            style = v3Typography.headingLarge,
        )
        Text(
            text = stringResource(R.string.stripe_networking_link_login_warmup_description),
            style = v3Typography.bodyMedium,
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun Footer(
    disableNetworkingAsync: Async<FinancialConnectionsSessionManifest>,
    onContinueClick: () -> Unit,
    onSkipClicked: () -> Unit
) {
    Column {
        FinancialConnectionsButton(
            loading = false,
            enabled = disableNetworkingAsync !is Loading,
            type = Type.Primary,
            onClick = onContinueClick,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("existing_email-button")
                .fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.stripe_networking_link_login_warmup_cta_continue))
        }
        Spacer(modifier = Modifier.size(16.dp))
        FinancialConnectionsButton(
            loading = false,
            enabled = disableNetworkingAsync !is Loading,
            type = Type.Secondary,
            onClick = { onSkipClicked() },
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("skip-button")
                .fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.stripe_networking_link_login_warmup_cta_skip))
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ExistingEmailSection(
    email: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTagsAsResourceId = true }
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = v3Colors.border,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(color = LinkColors.Brand200, shape = CircleShape)
        ) {
            Text(
                text = email.getOrElse(0) { 'A' }.uppercaseChar().toString(),
                style = v3Typography.bodySmall,
                color = LinkColors.Brand600
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = email,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = v3Typography.bodySmall,
            color = v3Colors.textDefault
        )
    }
}

@Composable
@Preview(group = "NetworkingLinkLoginWarmup Pane", name = "Canonical")
internal fun NetworkingLinkLoginWarmupScreenPreview(
    @PreviewParameter(NetworkingLinkLoginWarmupPreviewParameterProvider::class) state: NetworkingLinkLoginWarmupState
) {
    FinancialConnectionsPreview {
        NetworkingLinkLoginWarmupContent(
            state = state,
            onCloseClick = {},
            onContinueClick = {},
            onSkipClicked = {},
            onCloseFromErrorClick = {}
        )
    }
}
