package com.stripe.android.financialconnections.features.success

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.stripe.android.financialconnections.presentation.paneViewModel

@Composable
internal fun SuccessScreen() {
    val viewModel: SuccessViewModel = paneViewModel { SuccessViewModel.factory(it) }
    val state: State<SuccessState> = viewModel.stateFlow.collectAsState()
    BackHandler(enabled = true) {}
    SuccessContent(
        completeSessionAsync = state.value.completeSession,
        payloadAsync = state.value.payload,
        onDoneClick = viewModel::onDoneClick
    )
}
