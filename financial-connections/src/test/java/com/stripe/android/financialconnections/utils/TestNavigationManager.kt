package com.stripe.android.financialconnections.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.navigation.NavigationCommand
import com.stripe.android.financialconnections.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.assertIs

/**
 * Asserts that the [NavigationManager] is in a state where it will navigate to the given route.
 */

internal class TestNavigationManager : NavigationManager {

    val emittedEvents = mutableListOf<NavigationState>()

    override val navigationState: MutableStateFlow<NavigationState> =
        MutableStateFlow(NavigationState.Idle)

    override fun navigate(state: NavigationState) {
        emittedEvents.add(state)
    }

    override fun onNavigated(state: NavigationState) = Unit

    fun assertNavigatedTo(destination: NavigationCommand) {
        val last = emittedEvents.last()
        assertIs<NavigationState.NavigateToRoute>(last)
        assertThat(last.command.destination).isEqualTo(destination.destination)
    }
}
