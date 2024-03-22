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

    // TODO(tillh-stripe) Add <A> generic for ViewAction once all existing ViewModels
    //  are using them

    val topAppBarState: StateFlow<TopAppBarState> = topAppBarHost.topAppBarState

    init {
        updateHostWithTopAppBarState(initialState)

        onEach { state ->
            updateHostWithTopAppBarState(state)
        }
    }

    abstract fun updateTopAppBar(state: S): TopAppBarStateUpdate?

    open fun handleErrorAction(action: FinancialConnectionsErrorAction) {
        // TODO(tillh-stripe) Make this required once all existing ViewModels are implementing it
    }

    private fun updateHostWithTopAppBarState(state: S) {
        val update = updateTopAppBar(state)
        topAppBarHost.updateTopAppBarState(update)
    }
}
