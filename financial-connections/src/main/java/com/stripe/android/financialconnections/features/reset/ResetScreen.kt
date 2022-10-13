package com.stripe.android.financialconnections.features.reset

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun ResetScreen() {
    val viewModel: ResetViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val payload = viewModel.collectAsState { it.payload }
    BackHandler(enabled = true) {}
    ResetContent(
        payload = payload.value,
        onCloseClick = { parentViewModel.onCloseNoConfirmationClick(NextPane.RESET) },
        onBackClick = { parentViewModel.onBackClick(NextPane.RESET) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun ResetContent(
    payload: Async<Unit>,
    onCloseClick: () -> Unit,
    onBackClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                onCloseClick = onCloseClick,
                onBackClick = onBackClick
            )
        }
    ) {
        when (payload) {
            Uninitialized, is Loading -> LoadingContent()
            is Success -> LoadingContent()
            is Fail -> UnclassifiedErrorContent(
                error = payload.error,
                onCloseFromErrorClick = onCloseFromErrorClick
            )
        }
    }
}

@Composable
@Preview
internal fun ResetScreenPreview() {
    FinancialConnectionsTheme {
        ResetContent(
            payload = Uninitialized,
            onCloseClick = {},
            onCloseFromErrorClick = {},
            onBackClick = {}
        )
    }
}
