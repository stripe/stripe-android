package com.stripe.android.financialconnections.features.manualentrysuccess

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.success.SuccessContent

@Composable
internal fun ManualEntrySuccessScreen() {
    val viewModel: ManualEntrySuccessViewModel = mavericksViewModel()
    val state by viewModel.collectAsState()
    BackHandler(true) {}
    SuccessContent(
        completeSessionAsync = state.completeSession,
        payloadAsync = state.payload,
        onDoneClick = viewModel::onSubmit,
    )
}
