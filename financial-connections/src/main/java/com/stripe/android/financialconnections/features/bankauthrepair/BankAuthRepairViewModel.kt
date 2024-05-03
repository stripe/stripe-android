package com.stripe.android.financialconnections.features.bankauthrepair

import android.os.Parcelable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.utils.error
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.parcelize.Parcelize

internal class BankAuthRepairViewModel @AssistedInject constructor(
    @Assisted initialState: SharedPartnerAuthState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : FinancialConnectionsViewModel<SharedPartnerAuthState>(initialState, nativeAuthFlowCoordinator) {

    override fun updateTopAppBar(state: SharedPartnerAuthState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = Pane.BANK_AUTH_REPAIR,
            allowBackNavigation = state.canNavigateBack,
            error = state.payload.error,
        )
    }

    @Parcelize
    data class Args(val pane: Pane) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(initialState: SharedPartnerAuthState): BankAuthRepairViewModel
    }

    internal companion object {
        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.bankAuthRepairViewModelFactory.create(SharedPartnerAuthState(Args(PANE)))
                }
            }

        val PANE = Pane.BANK_AUTH_REPAIR
    }
}
