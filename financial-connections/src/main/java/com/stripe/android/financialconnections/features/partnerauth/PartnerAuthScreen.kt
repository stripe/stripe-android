package com.stripe.android.financialconnections.features.partnerauth

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@SuppressLint("SetJavaScriptEnabled")
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
    Column {
        Text(
            state.value.title,
            style = FinancialConnectionsTheme.typography.heading
        )
    }
}
