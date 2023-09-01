package com.stripe.android.financialconnections.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationIntent
import com.stripe.android.financialconnections.navigation.NavigationManager
import kotlinx.coroutines.channels.Channel
import kotlin.test.assertIs

/**
 * Asserts that the [NavigationManager] is in a state where it will navigate to the given route.
 */

internal class TestNavigationManager : NavigationManager {

    val emittedIntents = mutableListOf<NavigationIntent>()

    override val navigationChannel: Channel<NavigationIntent>
        get() = Channel { }

    override suspend fun navigateBack(route: String?, inclusive: Boolean) {
        emittedIntents.add(
            NavigationIntent.NavigateBack(
                route = route,
                inclusive = inclusive
            )
        )
    }

    override fun tryNavigateBack(route: String?, inclusive: Boolean) {
        emittedIntents.add(
            NavigationIntent.NavigateBack(
                route = route,
                inclusive = inclusive
            )
        )
    }

    override suspend fun navigateTo(
        route: String,
        popUpToCurrent: Boolean,
        inclusive: Boolean,
        isSingleTop: Boolean
    ) {
        emittedIntents.add(
            NavigationIntent.NavigateTo(
                route = route,
                popUpToCurrent = popUpToCurrent,
                inclusive = inclusive,
                isSingleTop = isSingleTop,
            )
        )
    }

    override fun tryNavigateTo(
        route: String,
        popUpToCurrent: Boolean,
        inclusive: Boolean,
        isSingleTop: Boolean
    ) {
        emittedIntents.add(
            NavigationIntent.NavigateTo(
                route = route,
                popUpToCurrent = popUpToCurrent,
                inclusive = inclusive,
                isSingleTop = isSingleTop,
            )
        )
    }

    fun assertNavigatedTo(destination: Destination) {
        val last: NavigationIntent = emittedIntents.last()
        assertIs<NavigationIntent.NavigateTo>(last)
        assertThat(last.route).isEqualTo(destination.fullRoute)
    }
}
