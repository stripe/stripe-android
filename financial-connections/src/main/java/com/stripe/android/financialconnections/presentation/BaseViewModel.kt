package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.stripe.android.financialconnections.ui.components.TopAppBarState

internal interface TopAppBarHost {
    fun handleTopAppBarStateChanged(topAppBarState: TopAppBarState)
}

internal abstract class BaseViewModel<S : MavericksState>(
    initialState: S,
    topAppBarHost: TopAppBarHost,
) : MavericksViewModel<S>(initialState) {

    init {
        onEach { state ->
            val topAppBarState = mapStateToTopAppBarState(state)
            topAppBarHost.handleTopAppBarStateChanged(topAppBarState)
        }
    }

    abstract fun mapStateToTopAppBarState(state: S): TopAppBarState
}
