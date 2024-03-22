package com.stripe.android.financialconnections.features.success

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel

@Composable
internal fun SuccessScreen() {
    val viewModel: SuccessViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state: State<SuccessState> = viewModel.collectAsState()
    val topAppBarState by parentViewModel.topAppBarState.collectAsState()
    BackHandler(enabled = true) {}
    SuccessContent(
        completeSessionAsync = state.value.completeSession,
        payloadAsync = state.value.payload,
        topAppBarState = topAppBarState,
        onDoneClick = viewModel::onDoneClick
    ) { parentViewModel.onCloseNoConfirmationClick(Pane.SUCCESS) }
}
