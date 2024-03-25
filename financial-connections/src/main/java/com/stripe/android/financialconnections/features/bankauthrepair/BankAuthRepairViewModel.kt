package com.stripe.android.financialconnections.features.bankauthrepair

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.financialconnections.core.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import javax.inject.Inject

internal class BankAuthRepairViewModel @Inject constructor(
    initialState: SharedPartnerAuthState
) : FinancialConnectionsViewModel<SharedPartnerAuthState>(initialState) {

    internal companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent
                        .bankAuthRepairSubcomponent
                        .initialState(SharedPartnerAuthState(pane = PANE, savedState = null))
                        .build()
                        .viewModel
                }
            }

        val PANE = Pane.BANK_AUTH_REPAIR
    }
}
