package com.stripe.android.financialconnections.features.success

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.uicore.utils.collectAsState

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
