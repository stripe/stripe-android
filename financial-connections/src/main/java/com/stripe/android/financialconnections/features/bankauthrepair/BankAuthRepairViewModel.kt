package com.stripe.android.financialconnections.features.bankauthrepair

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class BankAuthRepairViewModel @Inject constructor(
    initialState: SharedPartnerAuthState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : FinancialConnectionsViewModel<SharedPartnerAuthState>(initialState, nativeAuthFlowCoordinator) {

    internal companion object :
        MavericksViewModelFactory<BankAuthRepairViewModel, SharedPartnerAuthState> {

        override fun initialState(viewModelContext: ViewModelContext) =
            SharedPartnerAuthState(pane = PANE)

        override fun create(
            viewModelContext: ViewModelContext,
            state: SharedPartnerAuthState
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
