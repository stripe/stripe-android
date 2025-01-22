package com.stripe.android.financialconnections.features.bankauthrepair

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.stripe.android.financialconnections.features.common.SharedPartnerAuth
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun BankAuthRepairScreen() {
    // step view model
    val viewModel: BankAuthRepairViewModel = paneViewModel { BankAuthRepairViewModel.factory(it) }
    val state: State<SharedPartnerAuthState> = viewModel.stateFlow.collectAsState()

    SharedPartnerAuth(
        state = state.value,
        onContinueClick = viewModel::onLaunchAuthClick,
        onCancelClick = viewModel::onCancelClick,
        onClickableTextClick = viewModel::onClickableTextClick,
        onWebAuthFlowFinished = viewModel::onWebAuthFlowFinished,
        onViewEffectLaunched = viewModel::onViewEffectLaunched,
        inModal = false
    )
}
