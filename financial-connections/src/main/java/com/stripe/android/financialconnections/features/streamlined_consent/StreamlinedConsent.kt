package com.stripe.android.financialconnections.features.streamlined_consent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.generic.GenericScreen
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.uicore.utils.collectAsState

@Composable
fun StreamlinedConsent(modifier: Modifier = Modifier) {
    val viewModel: StreamlinedConsentViewModel = paneViewModel { StreamlinedConsentViewModel.factory(it) }
    val state by viewModel.stateFlow.collectAsState()

    val parentViewModel = parentViewModel()

    StreamlinedConsentContent(
        state = state,
        onPrimaryButtonClick = viewModel::onContinueClick,
        onSecondaryButtonClick = {},
        onClickableTextClick = viewModel::onClickableTextClick,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
    )
}

@Composable
private fun StreamlinedConsentContent(
    state: StreamlinedConsentState,
    onPrimaryButtonClick: () -> Unit,
    onSecondaryButtonClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
) {
    when (val result = state.payload) {
        Uninitialized,
        is Loading -> {
            FullScreenGenericLoading()
        }

        is Success -> {
            GenericScreen(
                state = result().genericScreenState,
                onPrimaryButtonClick = onPrimaryButtonClick,
                onSecondaryButtonClick = onSecondaryButtonClick,
                onClickableTextClick = onClickableTextClick,
            )
        }

        is Fail -> {
            UnclassifiedErrorContent(
                onCtaClick = { onCloseFromErrorClick(result.error) },
            )
        }
    }
}
