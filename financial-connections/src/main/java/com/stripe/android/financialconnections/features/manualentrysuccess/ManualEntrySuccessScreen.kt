package com.stripe.android.financialconnections.features.manualentrysuccess

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.stripe.android.financialconnections.features.success.SuccessContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel

@Composable
internal fun ManualEntrySuccessScreen() {
    val parentViewModel = parentViewModel()
    val viewModel: ManualEntrySuccessViewModel = paneViewModel { ManualEntrySuccessViewModel.factory(it) }
    val state by viewModel.stateFlow.collectAsState()
    val topAppBarState by parentViewModel.topAppBarState.collectAsState()
    BackHandler(true) {}
    SuccessContent(
        completeSessionAsync = state.completeSession,
        payloadAsync = state.payload,
        topAppBarState = topAppBarState,
        onDoneClick = viewModel::onSubmit,
        onCloseClick = {
            parentViewModel.onCloseNoConfirmationClick(
                FinancialConnectionsSessionManifest.Pane.MANUAL_ENTRY_SUCCESS
            )
        }
    )
}
