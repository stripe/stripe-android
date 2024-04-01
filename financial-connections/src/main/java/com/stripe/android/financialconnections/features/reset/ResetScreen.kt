package com.stripe.android.financialconnections.features.reset

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarState
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar

@Composable
internal fun ResetScreen() {
    val viewModel: ResetViewModel = paneViewModel {
        ResetViewModel.factory(it)
    }
    val parentViewModel = parentViewModel()
    val state by viewModel.stateFlow.collectAsState()
    val topAppBarState by parentViewModel.topAppBarState.collectAsState()
    BackHandler(enabled = true) {}
    ResetContent(
        payload = state.payload,
        topAppBarState = topAppBarState,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.RESET) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun ResetContent(
    payload: Async<Unit>,
    topAppBarState: TopAppBarState,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                state = topAppBarState,
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (payload) {
            Uninitialized, is Loading, is Success -> FullScreenGenericLoading()
            is Fail -> UnclassifiedErrorContent { onCloseFromErrorClick(payload.error) }
        }
    }
}

@Preview(
    group = "Reset",
    name = "Default"
)
@Composable
internal fun ResetScreenPreview() {
    FinancialConnectionsPreview {
        ResetContent(
            payload = Uninitialized,
            topAppBarState = TopAppBarState(hideStripeLogo = false),
            onCloseClick = {},
            onCloseFromErrorClick = {},
        )
    }
}
