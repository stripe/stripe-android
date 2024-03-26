package com.stripe.android.financialconnections.features.partnerauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.stripe.android.financialconnections.core.paneViewModel
import com.stripe.android.financialconnections.features.common.SharedPartnerAuth

@Composable
internal fun PartnerAuthScreen(inModal: Boolean) {
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
