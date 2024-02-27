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
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.ShapedIcon
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton.Type
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.LazyLayout
import com.stripe.android.financialconnections.ui.theme.LinkColors

@Composable
internal fun NetworkingLinkLoginWarmupScreen() {
    val viewModel: NetworkingLinkLoginWarmupViewModel = mavericksViewModel()
    val state = viewModel.collectAsState()
    NetworkingLinkLoginWarmupContent(
        state = state.value,
        onSkipClicked = viewModel::onSkipClicked,
        onContinueClick = viewModel::onContinueClick,
    )
}

@Composable
private fun NetworkingLinkLoginWarmupContent(
    state: NetworkingLinkLoginWarmupState,
    onContinueClick: () -> Unit,
    onSkipClicked: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    LazyLayout(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colors.backgroundSurface)
            .padding(top = 24.dp),
        inModal = true,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        lazyListState = lazyListState,
        body = {
            item { HeaderSection() }
            item { ExistingEmailSection(email = state.payload()?.email ?: "") }
        },
        footer = {
            Footer(
                loading = state.disableNetworkingAsync is Loading || state.payload() == null,
                onContinueClick = onContinueClick,
                onSkipClicked = onSkipClicked
            )
        }
    )
}

@Composable
private fun HeaderSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShapedIcon(
            painter = painterResource(id = R.drawable.stripe_ic_person),
            contentDescription = stringResource(R.string.stripe_networking_link_login_warmup_title)
        )
        Text(
            text = stringResource(R.string.stripe_networking_link_login_warmup_title),
            style = typography.headingLarge,
        )
        Text(
            text = stringResource(R.string.stripe_networking_link_login_warmup_description),
            style = typography.bodyMedium,
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun Footer(
    loading: Boolean,
    onContinueClick: () -> Unit,
    onSkipClicked: () -> Unit
) {
    Column {
        FinancialConnectionsButton(
            loading = false,
            enabled = loading.not(),
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
            enabled = loading.not(),
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
                color = colors.border,
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
                text = email.getOrElse(0) { '@' }.uppercaseChar().toString(),
                style = typography.bodySmall,
                color = LinkColors.Brand600
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = email,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.bodySmall,
            color = colors.textDefault
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
            onContinueClick = {},
            onSkipClicked = {},
        )
    }
}
