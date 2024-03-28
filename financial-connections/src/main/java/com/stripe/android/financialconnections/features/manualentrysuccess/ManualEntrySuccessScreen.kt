package com.stripe.android.financialconnections.features.manualentrysuccess

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.stripe.android.financialconnections.features.success.SuccessContent
import com.stripe.android.financialconnections.presentation.paneViewModel

@Composable
internal fun ManualEntrySuccessScreen() {
    val viewModel: ManualEntrySuccessViewModel = paneViewModel { ManualEntrySuccessViewModel.factory(it) }
    val state by viewModel.stateFlow.collectAsState()
    BackHandler(true) {}
    SuccessContent(
        completeSessionAsync = state.completeSession,
        payloadAsync = state.payload,
        onDoneClick = viewModel::onSubmit,
    )
}
