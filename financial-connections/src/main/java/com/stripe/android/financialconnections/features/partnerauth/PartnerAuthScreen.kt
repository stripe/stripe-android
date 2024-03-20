package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.common.SharedPartnerAuth
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane

@Composable
internal fun PartnerAuthScreen(inModal: Boolean) {
    val viewModel: PartnerAuthViewModel = mavericksViewModel(
        argsFactory = { PartnerAuthViewModel.Args(inModal, Pane.PARTNER_AUTH) }
    )
    val state: State<SharedPartnerAuthState> = viewModel.collectAsState()

    SharedPartnerAuth(
        inModal = inModal,
        state = state.value,
        onContinueClick = viewModel::onLaunchAuthClick,
        onCancelClick = viewModel::onCancelClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onWebAuthFlowFinished = viewModel::onWebAuthFlowFinished,
        onViewEffectLaunched = viewModel::onViewEffectLaunched
    )
}
