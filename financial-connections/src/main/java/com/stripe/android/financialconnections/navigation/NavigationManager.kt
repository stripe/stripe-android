package com.stripe.android.financialconnections.navigation

import com.stripe.android.core.Logger
import kotlinx.coroutines.flow.MutableStateFlow

internal interface NavigationManager {
    val navigationState: MutableStateFlow<NavigationState>
    fun navigate(state: NavigationState)
    fun onNavigated(state: NavigationState)
}

internal class NavigationManagerImpl(
    private val logger: Logger
): NavigationManager {


    override val navigationState: MutableStateFlow<NavigationState> =
        MutableStateFlow(NavigationState.Idle)

    override fun navigate(state: NavigationState) {
        logger.debug("NavigationManager navigating to: $navigationState")
        navigationState.value = state
    }

    override fun onNavigated(state: NavigationState) {
        // clear navigation state, if state is the current state:
        navigationState.compareAndSet(state, NavigationState.Idle)
    }
}