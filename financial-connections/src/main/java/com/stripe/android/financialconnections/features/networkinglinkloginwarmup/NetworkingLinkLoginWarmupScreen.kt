package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldSection

@Composable
internal fun NetworkingLinkLoginWarmupScreen() {
    val viewModel: NetworkingLinkLoginWarmupViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    NetworkingLinkLoginWarmupContent(
        state = state.value,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.NETWORKING_LINK_SIGNUP_PANE) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onClickableTextClick = viewModel::onClickableTextClick,
    )
}

@Composable
private fun NetworkingLinkLoginWarmupContent(
    state: NetworkingLinkLoginWarmupState,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onClickableTextClick: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
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
            is Success -> NetworkingLinkLoginWarmupLoaded(
                scrollState = scrollState,
                payload = payload(),
                onClickableTextClick = onClickableTextClick,
            )

            is Fail -> UnclassifiedErrorContent(
                error = payload.error,
                onCloseFromErrorClick = onCloseFromErrorClick
            )
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun NetworkingLinkLoginWarmupLoaded(
    scrollState: ScrollState,
    payload: Payload,
    onClickableTextClick: (String) -> Unit,
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
        AnnotatedText(
            text = TextResource.Text("Sign in to Link with Stripe"),
            defaultStyle = FinancialConnectionsTheme.typography.subtitle,
            annotationStyles = mapOf(
                StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.subtitle
                    .toSpanStyle()
                    .copy(color = FinancialConnectionsTheme.colors.textBrand),
            ),
            onClickableTextClick = {},
        )
        Spacer(modifier = Modifier.size(8.dp))
        AnnotatedText(
            text = TextResource.Text("It looks like you have a Link with Stripe account. Signing in will let you quickly access your saved bank accounts."),
            defaultStyle = FinancialConnectionsTheme.typography.body.copy(
                color = FinancialConnectionsTheme.colors.textSecondary
            ),
            annotationStyles = mapOf(
                StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.body
                    .toSpanStyle()
                    .copy(color = FinancialConnectionsTheme.colors.textBrand),
            ),
            onClickableTextClick = {},
        )
        Spacer(modifier = Modifier.size(24.dp))
        EmailCollectionSection(
            email = payload.email
        )
        AnnotatedText(
            text = TextResource.Text("Not you? Continue without signing in"),
            defaultStyle = FinancialConnectionsTheme.typography.caption.copy(
                color = FinancialConnectionsTheme.colors.textSecondary
            ),
            annotationStyles = mapOf(
                StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.caption
                    .toSpanStyle()
                    .copy(color = FinancialConnectionsTheme.colors.textBrand),
            ),
            onClickableTextClick = {},
        )
    }
}

@Composable
internal fun EmailCollectionSection(
    email: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        TextFieldSection(
            imeAction = ImeAction.Done,
            enabled = true,
            textFieldController = SimpleTextFieldController(
                EmailConfig(),
                initialValue = email
            ),
        )
    }
}

@Composable
@Preview(group = "NetworkingLinkLoginWarmup Pane", name = "Entering email")
internal fun NetworkingLinkLoginWarmupScreenEnteringEmailPreview() {
    FinancialConnectionsPreview {
        NetworkingLinkLoginWarmupContent(
            state = NetworkingLinkLoginWarmupState(
                payload = Success(
                    Payload(
                        merchantName = "Test",
                        email = "test@test.com",
                    )
                ),
            ),
            onCloseClick = {},
            onClickableTextClick = {},
            onCloseFromErrorClick = {}
        )
    }
}

