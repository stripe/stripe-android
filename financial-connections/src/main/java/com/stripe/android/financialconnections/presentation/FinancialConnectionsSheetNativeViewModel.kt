package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.di.DaggerFinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.di.FinancialConnectionsSubcomponentBuilderProvider
import com.stripe.android.financialconnections.domain.UpdateManifest
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.navigation.NavigationManager
import javax.inject.Inject

internal class FinancialConnectionsSheetNativeViewModel @Inject constructor(
    val navigationManager: NavigationManager,
    val subcomponentBuilderProvider: FinancialConnectionsSubcomponentBuilderProvider,
    updateManifest: UpdateManifest,
    initialState: FinancialConnectionsSheetNativeState
) : MavericksViewModel<FinancialConnectionsSheetNativeState>(initialState) {

    init {
        updateManifest.flow.setOnEach { copy(manifest = it) }
    }

    companion object :
        MavericksViewModelFactory<FinancialConnectionsSheetNativeViewModel, FinancialConnectionsSheetNativeState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: FinancialConnectionsSheetNativeState
        ): FinancialConnectionsSheetNativeViewModel {
            return DaggerFinancialConnectionsSheetNativeComponent
                .builder()
                .application(viewModelContext.app())
                .configuration(state.configuration)
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
    val manifest: FinancialConnectionsSessionManifest,
    val configuration: FinancialConnectionsSheet.Configuration
) : MavericksState {

    @Suppress("Unused")
    /**
     * Used by Mavericks to build initial state based on args.
     */
    constructor(args: FinancialConnectionsSheetNativeActivityArgs) : this(
        manifest = args.manifest,
        configuration = args.configuration
    )
}
