package com.stripe.android.financialconnections.features.reset

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.financialconnections.core.Async
import com.stripe.android.financialconnections.core.Async.Fail
import com.stripe.android.financialconnections.core.Async.Loading
import com.stripe.android.financialconnections.core.Async.Success
import com.stripe.android.financialconnections.core.Async.Uninitialized
import com.stripe.android.financialconnections.core.paneViewModel
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
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
    BackHandler(enabled = true) {}
    ResetContent(
        payload = state.payload,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.RESET) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun ResetContent(
    payload: Async<Unit>,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
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
            onCloseClick = {}
        ) {}
    }
}
