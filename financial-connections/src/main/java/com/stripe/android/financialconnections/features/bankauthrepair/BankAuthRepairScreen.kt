@file:OptIn(ExperimentalMaterialApi::class)
@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.bankauthrepair

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.features.common.SharedPartnerAuth
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState

@Composable
internal fun BankAuthRepairScreen() {
    // step view model
    val viewModel: BankAuthRepairViewModel = mavericksViewModel()
    val state: State<SharedPartnerAuthState> = viewModel.collectAsState()

    SharedPartnerAuth(
        state = state.value,
        onContinueClick = { /*TODO*/ },
        onSelectAnotherBank = { /*TODO*/ },
        onClickableTextClick = { /*TODO*/ },
        onEnterDetailsManually = { /*TODO*/ },
        onWebAuthFlowFinished = { /*TODO*/ },
        onViewEffectLaunched = { /*TODO*/ }
    )
}
