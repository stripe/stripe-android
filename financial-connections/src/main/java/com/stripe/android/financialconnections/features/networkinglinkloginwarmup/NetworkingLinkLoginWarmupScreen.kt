package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource.StringId
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldIcon.Trailing
import com.stripe.android.uicore.elements.TextFieldSection
import kotlinx.coroutines.flow.MutableStateFlow

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
                onContinueClick = onContinueClick
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
        Spacer(modifier = Modifier.size(8.dp))
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
        Spacer(modifier = Modifier.size(24.dp))
        ExistingEmailSection(
            email = payload.email,
            onContinueClick = onContinueClick
        )
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
}

@Composable
internal fun ExistingEmailSection(
    email: String,
    onContinueClick: () -> Unit
) {
    TextFieldSection(
        imeAction = ImeAction.Done,
        enabled = true,
        textFieldController = SimpleTextFieldController(
            SimpleTextFieldConfig(
                label = R.string.stripe_networking_link_login_warmup_email_label,
                trailingIcon = MutableStateFlow(
                    Trailing(
                        R.drawable.stripe_ic_arrow_right_circle,
                        isTintable = true,
                        onClick = onContinueClick
                    )
                )
            ),
            initialValue = email
        ),
    )
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
            onContinueClick = {},
            onCloseFromErrorClick = {}
        )
    }
}
