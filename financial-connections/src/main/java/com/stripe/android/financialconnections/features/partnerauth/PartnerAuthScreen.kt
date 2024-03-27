package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.stripe.android.financialconnections.core.paneViewModel
import com.stripe.android.financialconnections.features.common.SharedPartnerAuth
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane

@Composable
internal fun PartnerAuthScreen(inModal: Boolean) {
    argsFactory = { PartnerAuthViewModel.Args(inModal, FinancialConnectionsSessionManifest.Pane.PARTNER_AUTH) }
    val viewModel: PartnerAuthViewModel = paneViewModel { PartnerAuthViewModel.factory(it) }
    val state: State<SharedPartnerAuthState> = viewModel.stateFlow.collectAsState()

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
