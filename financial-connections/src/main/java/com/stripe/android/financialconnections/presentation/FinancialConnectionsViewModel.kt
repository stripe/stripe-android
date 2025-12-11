package com.stripe.android.financialconnections.presentation

import androidx.lifecycle.viewModelScope
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.UpdateTopAppBar
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.uicore.presentation.ReactiveStateViewModel
import kotlinx.coroutines.launch

internal abstract class FinancialConnectionsViewModel<S>(
    initialState: S,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : ReactiveStateViewModel<S>(initialState) {

    init {
        updateHostWithTopAppBarState(initialState)
        viewModelScope.launch {
            stateFlow.collect(::updateHostWithTopAppBarState)
        }
    }

    abstract fun updateTopAppBar(state: S): TopAppBarStateUpdate?

    private fun updateHostWithTopAppBarState(state: S) {
        viewModelScope.launch {
            val update = updateTopAppBar(state) ?: return@launch
            nativeAuthFlowCoordinator().emit(UpdateTopAppBar(update))
        }
    }
}
