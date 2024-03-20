package com.stripe.android.financialconnections.features.bankauthrepair

import android.os.Parcelable
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.financialconnections.utils.parentViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

internal class BankAuthRepairViewModel @Inject constructor(
    initialState: SharedPartnerAuthState,
    topAppBarHost: TopAppBarHost,
) : FinancialConnectionsViewModel<SharedPartnerAuthState>(initialState, topAppBarHost) {

    override fun updateTopAppBar(state: SharedPartnerAuthState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = Pane.BANK_AUTH_REPAIR,
            allowBackNavigation = state.canNavigateBack,
            error = state.payload.error,
        )
    }

    @Parcelize
    data class Args(val pane: Pane) : Parcelable

    internal companion object :
        MavericksViewModelFactory<BankAuthRepairViewModel, SharedPartnerAuthState> {

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
    }
}
