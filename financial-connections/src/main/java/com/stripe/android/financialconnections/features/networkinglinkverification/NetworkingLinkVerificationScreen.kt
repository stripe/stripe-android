@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.financialconnections.features.networkinglinkverification

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import com.airbnb.mvrx.Async
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
import com.stripe.android.financialconnections.features.networkinglinkverification.NetworkingLinkVerificationState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Layout

@Composable
internal fun NetworkingLinkVerificationScreen() {
    val viewModel: NetworkingLinkVerificationViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state = viewModel.collectAsState()
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
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> FullScreenGenericLoading()
            is Success -> NetworkingLinkVerificationLoaded(
                payload = payload(),
                confirmVerificationAsync = state.confirmVerification,
                onCloseFromErrorClick = onCloseFromErrorClick
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
private fun NetworkingLinkVerificationLoaded(
    confirmVerificationAsync: Async<Unit>,
    payload: Payload,
    onCloseFromErrorClick: (Throwable) -> Unit,
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
    if (confirmVerificationAsync is Fail && confirmVerificationAsync.error !is OTPError) {
        UnclassifiedErrorContent(
            error = confirmVerificationAsync.error,
            onCloseFromErrorClick = onCloseFromErrorClick
        )
    } else Layout(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        body = {
            item { Header(payload) }
            item {
                VerificationSection(
                    focusRequester = focusRequester,
                    otpElement = payload.otpElement,
                    enabled = confirmVerificationAsync !is Loading,
                    confirmVerificationError = (confirmVerificationAsync as? Fail)?.error
                )
            }
            if (confirmVerificationAsync is Loading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        V3LoadingSpinner(Modifier.size(24.dp))
                    }
                }
            }
        }
    )
}

@Composable
private fun Header(payload: Payload) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.stripe_networking_verification_title),
            style = FinancialConnectionsTheme.v3Typography.headingXLarge,
        )
        Text(
            text = stringResource(R.string.stripe_networking_verification_desc, payload.phoneNumber),
            style = FinancialConnectionsTheme.v3Typography.bodyMedium,
        )
    }
}

@Composable
@Preview(group = "NetworkingLinkVerification Pane")
internal fun NetworkingLinkVerificationScreenPreview(
    @PreviewParameter(NetworkingLinkVerificationPreviewParameterProvider::class)
    state: NetworkingLinkVerificationState
) {
    FinancialConnectionsPreview {
        NetworkingLinkVerificationContent(
            state = state,
            onCloseClick = {},
            onCloseFromErrorClick = {}
        )
    }
}
