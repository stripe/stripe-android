@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.financialconnections.features.linkstepupverification

import androidx.activity.compose.BackHandler
import androidx.annotation.RestrictTo
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.common.VerificationSection
import com.stripe.android.financialconnections.features.linkstepupverification.LinkStepUpVerificationState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement

@Composable
internal fun LinkStepUpVerificationScreen() {
    val viewModel: LinkStepUpVerificationViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    LinkStepUpVerificationContent(
        state = state.value,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.NETWORKING_LINK_SIGNUP_PANE) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onClickableTextClick = viewModel::onClickableTextClick
    )
}

@Composable
private fun LinkStepUpVerificationContent(
    state: LinkStepUpVerificationState,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onClickableTextClick: (String) -> Unit
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
            Uninitialized, is Loading -> FullScreenGenericLoading()
            is Fail -> UnclassifiedErrorContent(
                error = payload.error,
                onCloseFromErrorClick = onCloseFromErrorClick
            )
            is Success -> LinkStepUpVerificationLoaded(
                scrollState = scrollState,
                payload = payload(),
                confirmVerificationAsync = state.confirmVerification,
                resendOtpAsync = state.resendOtp,
                onClickableTextClick = onClickableTextClick
            )
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun LinkStepUpVerificationLoaded(
    confirmVerificationAsync: Async<Unit>,
    resendOtpAsync: Async<Unit>,
    scrollState: ScrollState,
    payload: Payload,
    onClickableTextClick: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester: FocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
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
        Description(email = payload.email)
        Spacer(modifier = Modifier.size(24.dp))
        VerificationSection(
            focusRequester = focusRequester,
            otpElement = payload.otpElement,
            enabled = confirmVerificationAsync !is Loading,
            confirmVerificationError = (confirmVerificationAsync as? Fail)?.error
        )
        Spacer(modifier = Modifier.size(24.dp))
        EmailSubtext(
            email = payload.email,
            isLoading = resendOtpAsync is Loading,
            onClickableTextClick = onClickableTextClick
        )
    }
}

@Composable
private fun EmailSubtext(
    email: String,
    isLoading: Boolean,
    onClickableTextClick: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            TextResource.Text(email) to 1f,
            TextResource.Text("•") to null,
            TextResource.StringId(R.string.stripe_link_stepup_verification_resend_code) to null
        ).forEach { (text, weight) ->
            AnnotatedText(
                modifier = if (weight != null) Modifier.weight(weight, fill = false) else Modifier,
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                defaultStyle = FinancialConnectionsTheme.typography.caption.copy(
                    color = colors.textSecondary,
                ),
                annotationStyles = mapOf(
                    StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.captionEmphasized
                        .toSpanStyle()
                        .copy(color = if (isLoading) colors.textSecondary else colors.textBrand),
                ),
                onClickableTextClick = onClickableTextClick,
            )
        }
        if (isLoading) {
            CircularProgressIndicator(
                Modifier.size(12.dp),
                strokeWidth = 1.dp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun Description(email: String) {
    AnnotatedText(
        text = TextResource.Text(
            stringResource(
                R.string.stripe_link_stepup_verification_desc,
                email
            )
        ),
        defaultStyle = FinancialConnectionsTheme.typography.body.copy(
            color = colors.textSecondary
        ),
        annotationStyles = mapOf(
            StringAnnotation.BOLD to FinancialConnectionsTheme.typography.bodyEmphasized
                .toSpanStyle()
                .copy(color = colors.textSecondary),
        ),
        onClickableTextClick = {},
    )
}

@Composable
private fun Title() {
    AnnotatedText(
        text = TextResource.Text(
            stringResource(R.string.stripe_link_stepup_verification_title)
        ),
        defaultStyle = FinancialConnectionsTheme.typography.subtitle,
        annotationStyles = emptyMap(),
        onClickableTextClick = {},
    )
}

@Composable
@Preview(group = "LinkStepUpVerification Pane", name = "Canonical")
internal fun LinkStepUpVerificationScreenPreview() {
    FinancialConnectionsPreview {
        LinkStepUpVerificationContent(
            state = LinkStepUpVerificationState(
                payload = Success(
                    Payload(
                        email = "theLargestEmailYoulleverseeThatCouldBreakALayout@email.com",
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
            onCloseFromErrorClick = {},
            onClickableTextClick = {}
        )
    }
}

@Composable
@Preview(group = "LinkStepUpVerification Pane", name = "Resending code")
internal fun LinkStepUpVerificationScreenResendingCodePreview() {
    FinancialConnectionsPreview {
        LinkStepUpVerificationContent(
            state = LinkStepUpVerificationState(
                resendOtp = Loading(),
                payload = Success(
                    Payload(
                        email = "shortEmail@email.com",
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
            onCloseFromErrorClick = {},
            onClickableTextClick = {}
        )
    }
}
