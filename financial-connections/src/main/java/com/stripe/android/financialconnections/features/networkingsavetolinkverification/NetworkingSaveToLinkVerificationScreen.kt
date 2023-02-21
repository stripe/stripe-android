@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.financialconnections.features.networkingsavetolinkverification

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
import com.stripe.android.financialconnections.features.networkingsavetolinkverification.NetworkingSaveToLinkVerificationState.Payload
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
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementUI

@Composable
internal fun NetworkingSaveToLinkVerificationScreen() {
    val viewModel: NetworkingSaveToLinkVerificationViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    NetworkingSaveToLinkVerificationContent(
        state = state.value,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.NETWORKING_LINK_SIGNUP_PANE) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
    )
}

@Composable
private fun NetworkingSaveToLinkVerificationContent(
    state: NetworkingSaveToLinkVerificationState,
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
            is Success -> NetworkingSaveToLinkVerificationLoaded(
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
@OptIn(ExperimentalComposeUiApi::class)
private fun NetworkingSaveToLinkVerificationLoaded(
    confirmVerificationAsync: Async<Unit>,
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
        Title()
        Spacer(modifier = Modifier.size(8.dp))
        Description(payload.phoneNumber)
        Spacer(modifier = Modifier.size(24.dp))
        ExistingEmailSection(
            focusRequester = focusRequester,
            otpElement = payload.otpElement,
            enabled = confirmVerificationAsync !is Loading
        )
        Spacer(modifier = Modifier.size(24.dp))
        EmailSubtext(payload.email)
        Spacer(modifier = Modifier.weight(1f))
        FinancialConnectionsButton(
            type = FinancialConnectionsButton.Type.Secondary,
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.stripe_networking_save_to_link_verification_cta_negative))
        }
    }
}

@Composable
private fun ExistingEmailSection(
    focusRequester: FocusRequester,
    otpElement: OTPElement,
    enabled: Boolean
) {
    StripeThemeForConnections {
        OTPElementUI(
            focusRequester = focusRequester,
            enabled = enabled,
            element = otpElement
        )
    }
}

@Composable
private fun EmailSubtext(email: String) {
    AnnotatedText(
        text = TextResource.Text(
            stringResource(
                R.string.stripe_networking_verification_email,
                email
            )
        ),
        defaultStyle = FinancialConnectionsTheme.typography.caption.copy(
            color = FinancialConnectionsTheme.colors.textDisabled
        ),
        annotationStyles = emptyMap(),
        onClickableTextClick = {},
    )
}

@Composable
private fun Description(phoneNumber: String) {
    AnnotatedText(
        text = TextResource.Text(
            stringResource(
                R.string.stripe_networking_verification_desc,
                phoneNumber
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
}

@Composable
private fun Title() {
    AnnotatedText(
        text = TextResource.Text(
            stringResource(R.string.stripe_networking_save_to_link_verification_title)
        ),
        defaultStyle = FinancialConnectionsTheme.typography.subtitle,
        annotationStyles = emptyMap(),
        onClickableTextClick = {},
    )
}

@Composable
@Preview(group = "NetworkingSaveToLinkVerification Pane", name = "Entering email")
internal fun NetworkingSaveToLinkVerificationScreenPreview() {
    FinancialConnectionsPreview {
        NetworkingSaveToLinkVerificationContent(
            state = NetworkingSaveToLinkVerificationState(
                payload = Success(
                    Payload(
                        email = "12345678",
                        phoneNumber = "12345678",
                        otpElement = OTPElement(
                            IdentifierSpec.Generic("otp"),
                            OTPController()
                        ),
                        consumerSessionClientSecret = "12345678"
                    )
                )
            ),
            onCloseClick = {},
            onCloseFromErrorClick = {}
        )
    }
}
