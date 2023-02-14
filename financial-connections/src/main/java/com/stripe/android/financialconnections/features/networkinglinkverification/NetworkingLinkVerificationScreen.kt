@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.financialconnections.features.networkinglinkverification

import androidx.activity.compose.BackHandler
import androidx.annotation.RestrictTo
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.networkinglinkverification.NetworkingLinkVerificationState.Payload
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
import com.stripe.android.financialconnections.ui.theme.StripeThemeForConnections
import com.stripe.android.model.ConsumerSession
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementUI

@Composable
internal fun NetworkingLinkVerificationScreen() {
    val viewModel: NetworkingLinkVerificationViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    NetworkingLinkVerificationContent(
        state = state.value,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.NETWORKING_LINK_SIGNUP_PANE) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
    )
}

@Composable
private fun NetworkingLinkVerificationContent(
    state: NetworkingLinkVerificationState,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
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
            is Success -> NetworkingLinkVerificationLoaded(
                scrollState = scrollState,
                payload = payload(),
                confirmVerificationAsync = state.confirmVerification
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
@OptIn(ExperimentalComposeUiApi::class)
private fun NetworkingLinkVerificationLoaded(
    confirmVerificationAsync: Async<ConsumerSession>,
    scrollState: ScrollState,
    payload: Payload,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester: FocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(confirmVerificationAsync) {
        if (confirmVerificationAsync is Loading) {
            focusManager.clearFocus(true)
            keyboardController?.hide()
        }
    }

    Column(
        Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
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
                text = TextResource.Text(
                    stringResource(R.string.stripe_networking_verification_title)
                ),
                defaultStyle = FinancialConnectionsTheme.typography.subtitle,
                annotationStyles = emptyMap(),
                onClickableTextClick = {},
            )
            Spacer(modifier = Modifier.size(8.dp))
            AnnotatedText(
                text = TextResource.Text(
                    stringResource(
                        R.string.stripe_networking_verification_desc,
                        payload.phoneNumber
                    )
                ),
                defaultStyle = FinancialConnectionsTheme.typography.body.copy(
                    color = FinancialConnectionsTheme.colors.textSecondary
                ),
                annotationStyles = mapOf(
                    StringAnnotation.BOLD to FinancialConnectionsTheme.typography.bodyEmphasized
                        .toSpanStyle()
                        .copy(color = FinancialConnectionsTheme.colors.textSecondary),
                ),
                onClickableTextClick = {},
            )
            Spacer(modifier = Modifier.size(24.dp))
            StripeThemeForConnections {
                OTPElementUI(
                    focusRequester = focusRequester,
                    enabled = confirmVerificationAsync !is Loading,
                    element = payload.otpElement
                )
            }
            Spacer(modifier = Modifier.size(24.dp))
            AnnotatedText(
                text = TextResource.Text(
                    stringResource(
                        R.string.stripe_networking_verification_email,
                        payload.email
                    )
                ),
                defaultStyle = FinancialConnectionsTheme.typography.caption.copy(
                    color = FinancialConnectionsTheme.colors.textDisabled
                ),
                annotationStyles = emptyMap(),
                onClickableTextClick = {},
            )
        }
        Spacer(modifier = Modifier.weight(1f))
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
                Text(text = stringResource(R.string.stripe_networking_signup_cta_negative))
            }
        }
    }
}

@Composable
@Preview(group = "NetworkingLinkVerification Pane", name = "Entering email")
internal fun NetworkingLinkVerificationScreenPreview() {
    FinancialConnectionsPreview {
        NetworkingLinkVerificationContent(
            state = NetworkingLinkVerificationState(
                payload = Success(
                    Payload(
                        phoneNumber = "12345678",
                        email = "12345678",
                        otpElement = OTPElement(
                            IdentifierSpec.Generic("otp"),
                            OTPController()
                        )
                    )
                )
            ),
            onCloseClick = {},
            onCloseFromErrorClick = {}
        )
    }
}
