@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.financialconnections.features.linkstepupverification

import androidx.activity.compose.BackHandler
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.LoadingSpinner
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.common.VerificationSection
import com.stripe.android.financialconnections.features.linkstepupverification.LinkStepUpVerificationState.Payload
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.LazyLayout
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun LinkStepUpVerificationScreen() {
    val viewModel: LinkStepUpVerificationViewModel = paneViewModel {
        LinkStepUpVerificationViewModel.factory(it)
    }
    val parentViewModel = parentViewModel()
    val state = viewModel.stateFlow.collectAsState()
    BackHandler(enabled = true) {}
    LinkStepUpVerificationContent(
        state = state.value,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onClickableTextClick = viewModel::onClickableTextClick
    )
}

@Composable
private fun LinkStepUpVerificationContent(
    state: LinkStepUpVerificationState,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onClickableTextClick: (String) -> Unit
) {
    val lazyListState = rememberLazyListState()
    Box {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> FullScreenGenericLoading()
            is Fail -> UnclassifiedErrorContent { onCloseFromErrorClick(payload.error) }
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
private fun LinkStepUpVerificationLoaded(
    lazyListState: LazyListState,
    submitError: Throwable?,
    submitLoading: Boolean,
    onCloseFromErrorClick: (Throwable) -> Unit,
    payload: Payload,
    onClickableTextClick: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val textInputService = LocalTextInputService.current

    val focusRequester: FocusRequester = remember { FocusRequester() }
    var shouldRequestFocus by rememberSaveable { mutableStateOf(false) }

    if (shouldRequestFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    LaunchedEffect(submitLoading) {
        if (submitLoading) {
            focusManager.clearFocus(true)
            @Suppress("DEPRECATION")
            textInputService?.hideSoftwareKeyboard()
        }
    }

    if (submitError != null && submitError !is OTPError) {
        UnclassifiedErrorContent { onCloseFromErrorClick(submitError) }
    } else {
        LazyLayout(
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
                        confirmVerificationError = submitError,
                        modifier = Modifier.onGloballyPositioned {
                            shouldRequestFocus = true
                        },
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
            style = typography.headingXLarge,
            color = colors.textDefault,
        )
        Text(
            text = stringResource(id = R.string.stripe_link_stepup_verification_desc, email),
            style = typography.bodyMedium,
            color = colors.textDefault,
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
            LoadingSpinner(modifier = Modifier.size(24.dp),)
        } else {
            AnnotatedText(
                text = TextResource.StringId(R.string.stripe_link_stepup_verification_resend_code),
                maxLines = 1,
                defaultStyle = typography.labelMedium,
                annotationStyles = mapOf(
                    StringAnnotation.CLICKABLE to typography.labelMediumEmphasized
                        .toSpanStyle()
                        .copy(color = colors.textAction),
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
            onCloseFromErrorClick = {},
            onClickableTextClick = {}
        )
    }
}
