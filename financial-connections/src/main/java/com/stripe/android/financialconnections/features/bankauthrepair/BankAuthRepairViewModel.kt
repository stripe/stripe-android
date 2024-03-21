package com.stripe.android.financialconnections.features.bankauthrepair

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.utils.parentViewModel
import javax.inject.Inject

internal class BankAuthRepairViewModel @Inject constructor(
    initialState: SharedPartnerAuthState,
    topAppBarHost: TopAppBarHost,
) : FinancialConnectionsViewModel<SharedPartnerAuthState>(initialState, topAppBarHost) {

    internal companion object :
        MavericksViewModelFactory<BankAuthRepairViewModel, SharedPartnerAuthState> {

        override fun initialState(viewModelContext: ViewModelContext) =
            SharedPartnerAuthState(pane = PANE)

        override fun create(
            viewModelContext: ViewModelContext,
            state: SharedPartnerAuthState
        ): BankAuthRepairViewModel {
            val parentViewModel = viewModelContext.parentViewModel()
            return parentViewModel
                .activityRetainedComponent
                .bankAuthRepairSubcomponent
                .initialState(state)
                .topAppBarHost(parentViewModel)
                .build()
                .viewModel
        }

        val PANE = Pane.BANK_AUTH_REPAIR
    }
}
