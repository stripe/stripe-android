package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import kotlinx.coroutines.launch

internal abstract class FinancialConnectionsViewModel<S : MavericksState>(
    initialState: S,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : MavericksViewModel<S>(initialState) {

    init {
        updateHostWithTopAppBarState(initialState)

        onEach { state ->
            updateHostWithTopAppBarState(state)
        }
    }

    abstract fun updateTopAppBar(state: S): TopAppBarStateUpdate?

    private fun updateHostWithTopAppBarState(state: S) {
        viewModelScope.launch {
            val update = updateTopAppBar(state) ?: return@launch
            nativeAuthFlowCoordinator().emit(NativeAuthFlowCoordinator.Message.UpdateTopAppBar(update))
        }
    }
}
