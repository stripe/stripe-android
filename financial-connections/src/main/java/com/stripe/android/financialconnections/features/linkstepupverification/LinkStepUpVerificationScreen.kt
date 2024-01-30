@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.financialconnections.features.linkstepupverification

import androidx.activity.compose.BackHandler
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.common.V3LoadingSpinner
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
import com.stripe.android.financialconnections.ui.components.elevation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import com.stripe.android.financialconnections.ui.theme.Layout

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
    val lazyListState = rememberLazyListState()
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = false,
                elevation = lazyListState.elevation,
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
                lazyListState = lazyListState,
                state.submitError,
                state.submitLoading,
                payload = payload(),
                onCloseFromErrorClick = onCloseFromErrorClick,
                onClickableTextClick = onClickableTextClick
            )
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun LinkStepUpVerificationLoaded(
    lazyListState: LazyListState,
    submitError: Throwable?,
    submitLoading: Boolean,
    onCloseFromErrorClick: (Throwable) -> Unit,
    payload: Payload,
    onClickableTextClick: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester: FocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(submitLoading) {
        if (submitLoading) {
            focusManager.clearFocus(true)
            keyboardController?.hide()
        }
    }
    if (submitError != null && submitError !is OTPError) {
        UnclassifiedErrorContent(
            error = submitError,
            onCloseFromErrorClick = onCloseFromErrorClick
        )
    } else Layout(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        lazyListState = lazyListState,
        body = {
            item {
                HeaderSection(payload.email)
            }
            item {
                VerificationSection(
                    focusRequester = focusRequester,
                    otpElement = payload.otpElement,
                    enabled = !submitLoading,
                    confirmVerificationError = submitError
                )
            }
            item {
                ResendCodeSection(
                    isLoading = submitLoading,
                    onClickableTextClick = onClickableTextClick
                )
            }
        }
    )
}

@Composable
private fun HeaderSection(
    email: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.stripe_link_stepup_verification_title),
            style = v3Typography.headingXLarge,
        )
        Text(
            text = stringResource(id = R.string.stripe_link_stepup_verification_desc, email),
            style = v3Typography.bodyMedium,
        )
    }
}

@Composable
private fun ResendCodeSection(
    isLoading: Boolean,
    onClickableTextClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            V3LoadingSpinner(modifier = Modifier.size(24.dp),)
        } else {
            AnnotatedText(
                text = TextResource.StringId(R.string.stripe_link_stepup_verification_resend_code),
                maxLines = 1,
                defaultStyle = v3Typography.labelMedium,
                annotationStyles = mapOf(
                    StringAnnotation.CLICKABLE to v3Typography.labelMediumEmphasized
                        .toSpanStyle()
                        .copy(color = v3Colors.textBrand),
                ),
                onClickableTextClick = onClickableTextClick,
            )
        }
    }
}

@Composable
@Preview
internal fun LinkStepUpVerificationPreview(
    @PreviewParameter(LinkStepUpVerificationPreviewParameterProvider::class) state: LinkStepUpVerificationState
) {
    FinancialConnectionsPreview {
        LinkStepUpVerificationContent(
            state = state,
            onCloseClick = {},
            onCloseFromErrorClick = {},
            onClickableTextClick = {}
        )
    }
}
