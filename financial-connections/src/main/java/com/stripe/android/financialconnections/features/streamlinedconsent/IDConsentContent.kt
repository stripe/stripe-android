package com.stripe.android.financialconnections.features.streamlinedconsent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
internal fun IDConsentContentScreen() {
    val viewModel: IDConsentContentViewModel = paneViewModel(IDConsentContentViewModel::factory)
    val parentViewModel = parentViewModel()

    val state by viewModel.stateFlow.collectAsState()

    IDConsentContent(
        state = state,
        onPrimaryButtonClick = viewModel::onContinueClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick,
    )
}

@Composable
private fun IDConsentContent(
    state: IDConsentContentState,
    onPrimaryButtonClick: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
) {
    when (val result = state.payload) {
        is Uninitialized,
        is Loading -> {
            FullScreenGenericLoading()
        }
        is Success -> {
            GenericScreen(
                state = result().genericScreenState,
                onPrimaryButtonClick = onPrimaryButtonClick,
                onSecondaryButtonClick = {
                    // There is no secondary button
                },
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
