package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.di.DaggerFinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.di.FinancialConnectionsSubcomponentBuilderProvider
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.navigation.NavigationManager
import javax.inject.Inject

internal class FinancialConnectionsSheetNativeViewModel @Inject constructor(
    val navigationManager: NavigationManager,
    val subcomponentBuilderProvider: FinancialConnectionsSubcomponentBuilderProvider,
    initialState: FinancialConnectionsSheetNativeState
) : MavericksViewModel<FinancialConnectionsSheetNativeState>(initialState) {

    companion object :
        MavericksViewModelFactory<FinancialConnectionsSheetNativeViewModel, FinancialConnectionsSheetNativeState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: FinancialConnectionsSheetNativeState
        ): FinancialConnectionsSheetNativeViewModel {
            return DaggerFinancialConnectionsSheetNativeComponent
                .builder()
                .application(viewModelContext.app())
                .internalArgs(state.initialArgs)
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

/**
 * Constructor used by Mavericks to build the initial state.
 */
internal data class FinancialConnectionsSheetNativeState(
    val initialArgs: FinancialConnectionsSheetActivityArgs,
) : MavericksState
