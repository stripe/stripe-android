@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.financialconnections.features.networkingsavetolinkverification

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.LoadingSpinner
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.common.VerificationSection
import com.stripe.android.financialconnections.features.networkingsavetolinkverification.NetworkingSaveToLinkVerificationState.Payload
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.LazyLayout
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun NetworkingSaveToLinkVerificationScreen() {
    val viewModel: NetworkingSaveToLinkVerificationViewModel = paneViewModel {
        NetworkingSaveToLinkVerificationViewModel.factory(it)
    }
    val parentViewModel = parentViewModel()
    val state = viewModel.stateFlow.collectAsState()
    NetworkingSaveToLinkVerificationContent(
        state = state.value,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
        onSkipClick = viewModel::onSkipClick
    )
}

@Composable
private fun NetworkingSaveToLinkVerificationContent(
    state: NetworkingSaveToLinkVerificationState,
    onSkipClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
) {
    Box {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> FullScreenGenericLoading()
            is Success -> NetworkingSaveToLinkVerificationLoaded(
                payload = payload(),
                confirmVerificationAsync = state.confirmVerification,
                onCloseFromErrorClick = onCloseFromErrorClick,
                onSkipClick = onSkipClick
            )

            is Fail -> UnclassifiedErrorContent { onCloseFromErrorClick(payload.error) }
        }
    }
}

@Composable
private fun NetworkingSaveToLinkVerificationLoaded(
    confirmVerificationAsync: Async<Unit>,
    payload: Payload,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onSkipClick: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val focusRequester: FocusRequester = remember { FocusRequester() }
    var shouldRequestFocus by rememberSaveable { mutableStateOf(false) }

    if (shouldRequestFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    LaunchedEffect(confirmVerificationAsync) {
        if (confirmVerificationAsync is Loading) {
            focusManager.clearFocus(true)
            keyboardController?.hide()
        }
    }

    if (shouldRequestFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    if (confirmVerificationAsync is Fail && confirmVerificationAsync.error !is ConfirmVerification.OTPError) {
        UnclassifiedErrorContent { onCloseFromErrorClick(confirmVerificationAsync.error) }
    } else {
        LazyLayout(
            lazyListState = lazyListState,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            body = {
                item { Header(payload) }
                item {
                    VerificationSection(
                        focusRequester = focusRequester,
                        otpElement = payload.otpElement,
                        enabled = confirmVerificationAsync !is Loading,
                        confirmVerificationError = confirmVerificationAsync.error,
                        modifier = Modifier.onGloballyPositioned {
                            shouldRequestFocus = true
                        },
                    )
                }
                if (confirmVerificationAsync is Loading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingSpinner(Modifier.size(24.dp))
                        }
                    }
                }
            },
            footer = if (payload.showNotNowButton) {
                {
                    FinancialConnectionsButton(
                        type = FinancialConnectionsButton.Type.Secondary,
                        onClick = onSkipClick,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.stripe_networking_save_to_link_verification_cta_negative))
                    }
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun Header(payload: Payload) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.stripe_networking_save_to_link_verification_title),
            style = FinancialConnectionsTheme.typography.headingXLarge,
            color = FinancialConnectionsTheme.colors.textDefault,
        )
        Text(
            text = stringResource(
                R.string.stripe_networking_verification_desc,
                payload.phoneNumber
            ),
            style = FinancialConnectionsTheme.typography.bodyMedium,
        )
    }
}

@Composable
@Preview
internal fun SaveToLinkVerificationPreview(
    @PreviewParameter(NetworkingSaveToLinkVerificationPreviewParameterProvider::class)
    state: NetworkingSaveToLinkVerificationState
) {
    FinancialConnectionsPreview {
        NetworkingSaveToLinkVerificationContent(
            state = state,
            onSkipClick = {},
            onCloseFromErrorClick = {}
        )
    }
}
