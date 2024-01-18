@file:Suppress("TooManyFunctions", "LongMethod")

package com.stripe.android.financialconnections.features.manualentrysuccess

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.success.SuccessContent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel

@Composable
internal fun ManualEntrySuccessScreen(
    backStackEntry: NavBackStackEntry,
) {
    val parentViewModel = parentViewModel()
    val viewModel: ManualEntrySuccessViewModel = mavericksViewModel(argsFactory = { backStackEntry.arguments })
    val state by viewModel.collectAsState()
    BackHandler(true) {}
    state.payload()?.let { payload ->
        SuccessContent(
            overrideAnimationForPreview = false,
            completeSessionAsync = state.completeSession,
            payload = payload,
            onDoneClick = viewModel::onSubmit,
            onCloseClick = { parentViewModel.onCloseNoConfirmationClick(Pane.MANUAL_ENTRY_SUCCESS) }
        )
    }
}
