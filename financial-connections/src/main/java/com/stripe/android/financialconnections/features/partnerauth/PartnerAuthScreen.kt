package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.stripe.android.financialconnections.features.common.SharedPartnerAuth
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun PartnerAuthScreen(inModal: Boolean) {
    val viewModel: PartnerAuthViewModel = paneViewModel {
        PartnerAuthViewModel.factory(
            parentComponent = it,
            args = PartnerAuthViewModel.Args(inModal, Pane.PARTNER_AUTH)
        )
    }
    val state by viewModel.stateFlow.collectAsState()

    SharedPartnerAuth(
        inModal = inModal,
        state = state,
        onContinueClick = viewModel::onLaunchAuthClick,
        onCancelClick = viewModel::onCancelClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onWebAuthFlowFinished = viewModel::onWebAuthFlowFinished,
        onViewEffectLaunched = viewModel::onViewEffectLaunched
    )
}
