package com.stripe.android.financialconnections.features.bankauthrepair

import android.os.Parcelable
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

internal class BankAuthRepairViewModel @Inject constructor(
    initialState: SharedPartnerAuthState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : FinancialConnectionsViewModel<SharedPartnerAuthState>(initialState, nativeAuthFlowCoordinator) {

    override fun updateTopAppBar(state: SharedPartnerAuthState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = Pane.BANK_AUTH_REPAIR,
            allowBackNavigation = state.canNavigateBack,
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
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .bankAuthRepairSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}
