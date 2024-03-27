package com.stripe.android.financialconnections.features.bankauthrepair

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.stripe.android.financialconnections.features.common.SharedPartnerAuth
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import com.stripe.android.financialconnections.presentation.paneViewModel

@Composable
internal fun BankAuthRepairScreen() {
    // step view model
    val viewModel: BankAuthRepairViewModel = paneViewModel { BankAuthRepairViewModel.factory(it) }
    val state: State<SharedPartnerAuthState> = viewModel.stateFlow.collectAsState()

    SharedPartnerAuth(
        state = state.value,
        onContinueClick = { /*TODO*/ },
        onCancelClick = { /*TODO*/ },
        onClickableTextClick = { /*TODO*/ },
        onWebAuthFlowFinished = { /*TODO*/ },
        onViewEffectLaunched = { /*TODO*/ },
        inModal = false
    )
}
