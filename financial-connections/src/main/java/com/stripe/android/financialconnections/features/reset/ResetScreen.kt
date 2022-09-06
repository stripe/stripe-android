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
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun ResetScreen() {
    val viewModel: ResetViewModel = mavericksViewModel()
    val state = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    ResetContent(state.value.payload)
}

@Composable
private fun ResetContent(payload: Async<Unit>) {
    FinancialConnectionsScaffold {
        when (payload) {
            Uninitialized, is Loading -> LoadingContent()
            is Success -> LoadingContent()
            is Fail -> UnclassifiedErrorContent()
        }
    }
}

@Composable
@Preview
internal fun ResetScreenPreview() {
    FinancialConnectionsTheme {
        ResetContent(Uninitialized)
    }
}
