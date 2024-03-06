package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.ui.components.TopAppBarState

internal interface TopAppBarHost {
    val initialTopAppBarState: TopAppBarState
    fun handleTopAppBarStateChanged(topAppBarState: TopAppBarState)
}

internal abstract class ScreenViewModel<S : MavericksState>(
    initialState: S,
    topAppBarHost: TopAppBarHost,
    pane: FinancialConnectionsSessionManifest.Pane,
) : MavericksViewModel<S>(initialState) {

    init {
        onEach { state ->
            val canGoBack = allowsBackNavigation(state)
            val hideLogo = hidesStripeLogo(
                state = state,
                // TODO Should this update?
                originalValue = topAppBarHost.initialTopAppBarState.hideStripeLogo
            )

            val topAppBarState = topAppBarHost.initialTopAppBarState.copy(
                pane = pane,
                hideStripeLogo = hideLogo,
                allowBackNavigation = canGoBack,
            )

            topAppBarHost.handleTopAppBarStateChanged(topAppBarState)
        }
    }

    abstract fun allowsBackNavigation(state: S): Boolean
    abstract fun hidesStripeLogo(state: S, originalValue: Boolean): Boolean
}
