package com.stripe.android.financialconnections.features.bankauthrepair

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.financialconnections.core.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
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
    TODO args
    internal companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent
                        .bankAuthRepairSubcomponent
                        .initialState(SharedPartnerAuthState(pane = PANE))
                        .build()
                        .viewModel
                }
            }

        val PANE = Pane.BANK_AUTH_REPAIR
    }
}
