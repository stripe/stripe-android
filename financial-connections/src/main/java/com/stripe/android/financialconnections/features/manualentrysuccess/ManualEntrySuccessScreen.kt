@file:Suppress("TooManyFunctions", "LongMethod")

package com.stripe.android.financialconnections.features.manualentrysuccess

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.success.SuccessContent
import com.stripe.android.financialconnections.features.success.SuccessState
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.parentViewModel

@Composable
internal fun ManualEntrySuccessScreen(
    backStackEntry: NavBackStackEntry,
) {
    val parentViewModel = parentViewModel()
    val viewModel: ManualEntrySuccessViewModel = mavericksViewModel()
    BackHandler(true) {}
    val completeAuthSessionAsync = viewModel
        .collectAsState(ManualEntrySuccessState::completeSession)
    SuccessContent(
        overrideAnimationForPreview = false,
        completeSessionAsync = completeAuthSessionAsync.value,
        payload = SuccessState.Payload(
            businessName = "test",
            accountsCount = 3,
            skipSuccessPane = false,
        ),
        onDoneClick = { viewModel.onSubmit() },
        onCloseClick = { parentViewModel.onCloseNoConfirmationClick(Pane.MANUAL_ENTRY_SUCCESS) }
    )
}

