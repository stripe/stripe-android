@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import androidx.annotation.RestrictTo
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
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
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupState.Payload
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupViewModel.Companion.PANE
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource.StringId
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.components.clickableSingle
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun NetworkingLinkLoginWarmupScreen() {
    val viewModel: NetworkingLinkLoginWarmupViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    NetworkingLinkLoginWarmupContent(
        state = state.value,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(PANE) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onContinueClick = viewModel::onContinueClick,
    )
}

@Composable
private fun NetworkingLinkLoginWarmupContent(
    state: NetworkingLinkLoginWarmupState,
    onCloseClick: () -> Unit,
    onContinueClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onClickableTextClick: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = true,
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> NetworkingLinkLoginWarmupLoading()
            is Success -> when (val disableNetworking = state.disableNetworkingAsync) {
                is Loading -> NetworkingLinkLoginWarmupLoading()
                is Uninitialized,
                is Success -> NetworkingLinkLoginWarmupLoaded(
                    scrollState = scrollState,
                    payload = payload(),
                    onClickableTextClick = onClickableTextClick,
                    onContinueClick = onContinueClick
                )

                is Fail -> UnclassifiedErrorContent(
                    error = disableNetworking.error,
                    onCloseFromErrorClick = onCloseFromErrorClick
                )
            }

            is Fail -> UnclassifiedErrorContent(
                error = payload.error,
                onCloseFromErrorClick = onCloseFromErrorClick
            )
        }
    }
}

@Composable
private fun NetworkingLinkLoginWarmupLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = FinancialConnectionsTheme.colors.iconBrand
        )
    }
}

@Composable
private fun NetworkingLinkLoginWarmupLoaded(
    scrollState: ScrollState,
    payload: Payload,
    onClickableTextClick: (String) -> Unit,
    onContinueClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                top = 0.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp
            )
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        Title(
            onClickableTextClick = onClickableTextClick
        )
        Spacer(modifier = Modifier.size(8.dp))
        Description(onClickableTextClick)
        Spacer(modifier = Modifier.size(24.dp))
        ExistingEmailSection(
            email = payload.email,
            onContinueClick = onContinueClick
        )
        Spacer(modifier = Modifier.size(20.dp))
        SkipSignIn(onClickableTextClick)
    }
}

@Composable
private fun SkipSignIn(onClickableTextClick: (String) -> Unit) {
    AnnotatedText(
        text = StringId(R.string.stripe_networking_link_login_warmup_skip),
        defaultStyle = FinancialConnectionsTheme.typography.caption.copy(
            color = FinancialConnectionsTheme.colors.textSecondary
        ),
        annotationStyles = mapOf(
            StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.captionEmphasized
                .toSpanStyle()
                .copy(color = FinancialConnectionsTheme.colors.textBrand),
        ),
        onClickableTextClick = onClickableTextClick,
    )
}

@Composable
private fun Description(onClickableTextClick: (String) -> Unit) {
    AnnotatedText(
        text = StringId(R.string.stripe_networking_link_login_warmup_description),
        defaultStyle = FinancialConnectionsTheme.typography.body.copy(
            color = FinancialConnectionsTheme.colors.textSecondary
        ),
        annotationStyles = mapOf(
            StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.body
                .toSpanStyle()
                .copy(color = FinancialConnectionsTheme.colors.textBrand),
        ),
        onClickableTextClick = onClickableTextClick,
    )
}

@Composable
private fun Title(onClickableTextClick: (String) -> Unit) {
    AnnotatedText(
        text = StringId(R.string.stripe_networking_link_login_warmup_title),
        defaultStyle = FinancialConnectionsTheme.typography.subtitle,
        annotationStyles = mapOf(
            StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.subtitle
                .toSpanStyle()
                .copy(color = FinancialConnectionsTheme.colors.textBrand),
        ),
        onClickableTextClick = onClickableTextClick,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ExistingEmailSection(
    email: String,
    onContinueClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTagsAsResourceId = true }
            .testTag("existing_email-button")
            .clickableSingle { onContinueClick() }
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = FinancialConnectionsTheme.colors.borderDefault,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(id = R.string.stripe_networking_link_login_warmup_email_label),
                style = FinancialConnectionsTheme.typography.caption,
                color = FinancialConnectionsTheme.colors.textSecondary
            )
            Text(
                text = email,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = FinancialConnectionsTheme.typography.body,
                color = FinancialConnectionsTheme.colors.textPrimary
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.stripe_ic_arrow_right_circle),
            tint = FinancialConnectionsTheme.colors.textBrand,
            contentDescription = null
        )
    }
}

@Composable
@Preview(group = "NetworkingLinkLoginWarmup Pane", name = "Canonical")
internal fun NetworkingLinkLoginWarmupScreenEnteringEmailPreview() {
    FinancialConnectionsPreview {
        NetworkingLinkLoginWarmupContent(
            state = NetworkingLinkLoginWarmupState(
                payload = Success(
                    Payload(
                        merchantName = "Test",
                        email = "verylongemailthatshouldellipsize@gmail.com",
                    )
                ),
            ),
            onCloseClick = {},
            onClickableTextClick = {},
            onContinueClick = {},
            onCloseFromErrorClick = {}
        )
    }
}
