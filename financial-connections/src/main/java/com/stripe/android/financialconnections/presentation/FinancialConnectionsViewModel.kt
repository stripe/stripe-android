package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarState
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import kotlinx.coroutines.flow.StateFlow

internal abstract class FinancialConnectionsViewModel<S : MavericksState>(
    initialState: S,
    private val topAppBarHost: TopAppBarHost,
) : MavericksViewModel<S>(initialState) {

    val topAppBarState: StateFlow<TopAppBarState> = topAppBarHost.topAppBarState

    init {
        updateHostWithTopAppBarState(initialState)

        onEach { state ->
            updateHostWithTopAppBarState(state)
        }
    }

    abstract fun updateTopAppBar(state: S): TopAppBarStateUpdate?

    private fun updateHostWithTopAppBarState(state: S) {
        val update = updateTopAppBar(state)
        topAppBarHost.updateTopAppBarState(update)
    }
}
