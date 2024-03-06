package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.stripe.android.financialconnections.ui.components.TopAppBarState

internal interface TopAppBarHost {
    val defaultTopAppBarState: TopAppBarState
    fun handleTopAppBarStateChanged(topAppBarState: TopAppBarState)
}

internal data class TopAppBarStateUpdate(
    val allowBackNavigation: Boolean? = null,
    val hideStripeLogo: Boolean? = null,
)

internal fun TopAppBarState.apply(update: TopAppBarStateUpdate): TopAppBarState {
    return copy(
        hideStripeLogo = update.hideStripeLogo ?: hideStripeLogo,
        allowBackNavigation = update.allowBackNavigation ?: allowBackNavigation,
    )
}

internal abstract class FullScreenViewModel<S : MavericksState>(
    initialState: S,
    topAppBarHost: TopAppBarHost,
) : MavericksViewModel<S>(initialState) {

    init {
        onEach { state ->
            val update = updateTopAppBarState(state)
            val topAppBarState = topAppBarHost.defaultTopAppBarState.apply(update)
            topAppBarHost.handleTopAppBarStateChanged(topAppBarState)
        }
    }

    abstract fun updateTopAppBarState(state: S): TopAppBarStateUpdate
}
