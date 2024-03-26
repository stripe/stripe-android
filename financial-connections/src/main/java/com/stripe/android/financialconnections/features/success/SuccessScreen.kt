package com.stripe.android.financialconnections.features.success

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.stripe.android.financialconnections.core.paneViewModel
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel

@Composable
internal fun SuccessScreen() {
    val viewModel: SuccessViewModel = paneViewModel { SuccessViewModel.factory(it) }
    val parentViewModel = parentViewModel()
    val state: State<SuccessState> = viewModel.stateFlow.collectAsState()
    BackHandler(enabled = true) {}
    SuccessContent(
        completeSessionAsync = state.value.completeSession,
        payloadAsync = state.value.payload,
        onDoneClick = viewModel::onDoneClick
    ) { parentViewModel.onCloseNoConfirmationClick(Pane.SUCCESS) }
}
