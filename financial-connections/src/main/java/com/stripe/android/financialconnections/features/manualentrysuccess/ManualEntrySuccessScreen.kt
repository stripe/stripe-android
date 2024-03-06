package com.stripe.android.financialconnections.features.manualentrysuccess

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.success.SuccessContent

@Composable
internal fun ManualEntrySuccessScreen(
    backStackEntry: NavBackStackEntry,
) {
    val viewModel: ManualEntrySuccessViewModel = mavericksViewModel(argsFactory = { backStackEntry.arguments })
    val state by viewModel.collectAsState()
    BackHandler(true) {}
    SuccessContent(
        completeSessionAsync = state.completeSession,
        payloadAsync = state.payload,
        onDoneClick = viewModel::onSubmit,
    )
}
