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
import androidx.compose.runtime.getValue
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
import androidx.navigation.NavBackStackEntry
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.FooterButton
import com.stripe.android.financialconnections.features.common.FooterButtons
import com.stripe.android.financialconnections.features.common.ShapedIcon
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.LazyLayout
import com.stripe.android.financialconnections.ui.theme.LinkBrand50
import com.stripe.android.financialconnections.ui.theme.LinkGreen200
import com.stripe.android.financialconnections.ui.theme.LinkGreen900
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.financialconnections.ui.theme.isLinkDs3
import com.stripe.android.model.LinkBrand
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun NetworkingLinkLoginWarmupScreen(
    backStackEntry: NavBackStackEntry
) {
    val viewModel: NetworkingLinkLoginWarmupViewModel = paneViewModel {
        NetworkingLinkLoginWarmupViewModel.factory(it, backStackEntry.arguments)
    }
    val state by viewModel.stateFlow.collectAsState()
    NetworkingLinkLoginWarmupContent(
        state = state,
        onSkipClicked = viewModel::onSecondaryButtonClicked,
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
            .background(color = colors.background),
        inModal = true,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        lazyListState = lazyListState,
        body = {
            item { HeaderSection(linkBrand = state.linkBrand) }
            item { ExistingEmailSection(email = state.payload()?.redactedEmail ?: "") }
        },
        footer = {
            Footer(
                primaryButtonLoading = state.continueAsync is Loading,
                secondaryButtonLoading = state.disableNetworkingAsync is Loading,
                secondaryButtonLabel = state.secondaryButtonLabel,
                linkBrand = state.linkBrand,
                onContinueClick = onContinueClick,
                onSkipClicked = onSkipClicked,
            )
        }
    )
}

@Composable
private fun HeaderSection(linkBrand: LinkBrand) {
    val isLinkDs3 = FinancialConnectionsTheme.theme.isLinkDs3
    val title = when {
        // DS 3.0 uses a fixed greeting with no brand name.
        isLinkDs3 -> stringResource(R.string.stripe_networking_link_login_warmup_title_welcome_back)
        linkBrand == LinkBrand.Link -> stringResource(R.string.stripe_networking_link_login_warmup_title)
        else -> stringResource(
            R.string.stripe_networking_link_login_warmup_title_with_brand,
            linkBrand.brandName()
        )
    }
    val description = if (linkBrand == LinkBrand.Link) {
        stringResource(R.string.stripe_networking_link_login_warmup_description)
    } else {
        stringResource(R.string.stripe_networking_link_login_warmup_description_with_brand, linkBrand.brandName())
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (isLinkDs3.not()) {
            // DS 3.0 removes the avatar above the title entirely.
            ShapedIcon(
                painter = painterResource(id = R.drawable.stripe_ic_person),
                contentDescription = title
            )
        }
        Text(
            text = title,
            style = typography.headingLarge,
            color = colors.textDefault,
        )
        Text(
            text = description,
            style = typography.bodyMedium,
            color = colors.textDefault,
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun Footer(
    primaryButtonLoading: Boolean,
    secondaryButtonLoading: Boolean,
    secondaryButtonLabel: Int,
    linkBrand: LinkBrand,
    onContinueClick: () -> Unit,
    onSkipClicked: () -> Unit
) {
    val enableButtons = !primaryButtonLoading && !secondaryButtonLoading
    val ctaContinue = when {
        // The side-by-side layout only has room for the short label.
        FinancialConnectionsTheme.theme.isLinkDs3 ->
            stringResource(id = R.string.stripe_networking_link_login_warmup_cta_continue_short)
        linkBrand == LinkBrand.Link ->
            stringResource(id = R.string.stripe_networking_link_login_warmup_cta_continue)
        else -> stringResource(
            id = R.string.stripe_networking_link_login_warmup_cta_continue_with_brand,
            linkBrand.brandName()
        )
    }

    FooterButtons(
        preferSideBySide = true,
        stackedSpacing = 16.dp,
        primary = FooterButton(
            onClick = onContinueClick,
            enabled = enableButtons,
            loading = primaryButtonLoading,
            testTag = "existing_email-button",
            content = { Text(text = ctaContinue) },
        ),
        secondary = FooterButton(
            onClick = onSkipClicked,
            enabled = enableButtons,
            loading = secondaryButtonLoading,
            testTag = "skip-button",
            content = { Text(text = stringResource(id = secondaryButtonLabel)) },
        ),
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ExistingEmailSection(
    email: String
) {
    val isLinkDs3 = FinancialConnectionsTheme.theme.isLinkDs3
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTagsAsResourceId = true }
            .clip(RoundedCornerShape(12.dp))
            // DS 3.0 replaces the outline with a green-tinted fill.
            .then(
                if (isLinkDs3) {
                    Modifier.background(color = LinkBrand50, shape = RoundedCornerShape(12.dp))
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = colors.borderNeutral,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(color = LinkGreen200, shape = CircleShape)
        ) {
            Text(
                text = email.getOrElse(0) { '@' }.uppercaseChar().toString(),
                style = typography.bodySmall,
                color = LinkGreen900,
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = email,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.bodySmall,
            color = if (isLinkDs3) LinkGreen200 else colors.textDefault
        )
    }
}

@Composable
@Preview(group = "NetworkingLinkLoginWarmup Pane", name = "Link DS 3.0")
internal fun NetworkingLinkLoginWarmupScreenLinkDs3Preview() {
    FinancialConnectionsPreview(theme = Theme.LinkDs3) {
        NetworkingLinkLoginWarmupContent(
            state = NetworkingLinkLoginWarmupPreviewParameterProvider().instantDebits(),
            onContinueClick = {},
            onSkipClicked = {},
        )
    }
}

@Composable
@Preview(group = "NetworkingLinkLoginWarmup Pane", name = "Canonical")
internal fun NetworkingLinkLoginWarmupScreenPreview(
    @PreviewParameter(NetworkingLinkLoginWarmupPreviewParameterProvider::class) state: NetworkingLinkLoginWarmupState
) {
    FinancialConnectionsPreview(
        theme = if (state.isInstantDebits) Theme.LinkLight else Theme.DefaultLight,
    ) {
        NetworkingLinkLoginWarmupContent(
            state = state,
            onContinueClick = {},
            onSkipClicked = {},
        )
    }
}
