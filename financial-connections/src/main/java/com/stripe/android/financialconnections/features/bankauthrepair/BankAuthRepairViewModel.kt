package com.stripe.android.financialconnections.features.bankauthrepair

import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class BankAuthRepairViewModel @Inject constructor(
    initialState: PartnerAuthState
) : MavericksViewModel<PartnerAuthState>(initialState) {

    internal companion object :
        MavericksViewModelFactory<BankAuthRepairViewModel, PartnerAuthState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: PartnerAuthState
        ): BankAuthRepairViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .bankAuthRepairSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }

        val PANE = Pane.BANK_AUTH_REPAIR
    }
}
