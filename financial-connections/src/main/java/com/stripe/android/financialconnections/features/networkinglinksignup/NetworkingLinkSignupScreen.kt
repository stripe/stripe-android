package com.stripe.android.financialconnections.features.networkinglinksignup

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun NetworkingLinkSignupScreen() {
    val viewModel: NetworkingLinkSignupViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val payload = viewModel.collectAsState { it.payload }
    NetworkingLinkSignupContent(
        payload = payload.value,
        onCloseClick = { parentViewModel.onCloseNoConfirmationClick(Pane.RESET) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun NetworkingLinkSignupContent(
    payload: Async<Unit>,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    val scrollState = rememberScrollState()
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (payload) {
            Uninitialized, is Loading -> LoadingContent()
            is Success -> NetworkingLinkSignupLoaded(
                scrollState = scrollState
            )

            is Fail -> UnclassifiedErrorContent(
                error = payload.error,
                onCloseFromErrorClick = onCloseFromErrorClick
            )
        }
    }
}

@Composable
fun NetworkingLinkSignupLoaded(scrollState: ScrollState) {
    Column(
        Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
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
                text = TextResource.Text("Save your accounts to Link"),
                defaultStyle = FinancialConnectionsTheme.typography.subtitle,
                annotationStyles = mapOf(
                    StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.subtitle
                        .toSpanStyle()
                        .copy(color = FinancialConnectionsTheme.colors.textBrand),
                ),
                onClickableTextClick = {},
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Column(
            modifier = Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                top = 16.dp,
                bottom = 24.dp
            )
        ) {
            FinancialConnectionsButton(
                type = FinancialConnectionsButton.Type.Secondary,
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = "No thanks")
            }
        }
    }
}

@Composable
@Preview
internal fun NetworkingLinkSignupScreenPreview() {
    FinancialConnectionsPreview {
        NetworkingLinkSignupContent(
            payload = Success(Unit),
            onCloseClick = {}
        ) {}
    }
}
