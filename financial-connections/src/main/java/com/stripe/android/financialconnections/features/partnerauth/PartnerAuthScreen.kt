package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.institutionpicker.LoadingContent
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold

@Composable
internal fun PartnerAuthScreen() {
    // activity view model
    val activityViewModel: FinancialConnectionsSheetNativeViewModel = mavericksActivityViewModel()
    val authSession = activityViewModel.collectAsState { it.authorizationSession }
    val webAuthFlow = activityViewModel.collectAsState { it.webAuthFlow }

    // step view model
    val viewModel: PartnerAuthViewModel = mavericksViewModel()
    val state: State<PartnerAuthState> = viewModel.collectAsState()

    LaunchedEffect(authSession.value?.url) {
        val url = authSession.value?.url
        if (url != null) activityViewModel.openPartnerAuthFlowInBrowser(url)
    }
    LaunchedEffect(webAuthFlow.value) {
        viewModel.onWebAuthFlowFinished(webAuthFlow.value, authSession.value!!)
    }
    PartnerAuthScreenContent(state)
}

@Composable
private fun PartnerAuthScreenContent(state: State<PartnerAuthState>) {
    FinancialConnectionsScaffold {
        when (state.value.authenticationStatus) {
            Uninitialized, is Loading, is Success -> LoadingContent(
                titleResId = R.string.stripe_picker_loading_title,
                contentResId = R.string.stripe_picker_loading_desc
            )
            is Fail -> {
                // TODO@carlosmuvi translate error type to specific error screen.
                UnclassifiedErrorContent()
            }
        }
    }
}
