package com.stripe.android.financialconnections.features.success

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel

@Composable
internal fun SuccessScreen() {
    val viewModel: SuccessViewModel = mavericksViewModel()
    val state: State<SuccessState> = viewModel.collectAsState()
    BackHandler(enabled = true) {}
    SuccessContent(
        completeSessionAsync = state.value.completeSession,
        payloadAsync = state.value.payload,
        onDoneClick = viewModel::onDoneClick
    )
}
