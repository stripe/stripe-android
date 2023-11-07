package com.stripe.android.financialconnections.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationIntent
import com.stripe.android.financialconnections.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.test.assertIs

/**
 * Asserts that the [NavigationManager] is in a state where it will navigate to the given route.
 */

internal class TestNavigationManager : NavigationManager {

    val emittedIntents = mutableListOf<NavigationIntent>()

    override val navigationFlow: SharedFlow<NavigationIntent>
        get() = MutableSharedFlow()

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

    fun assertNavigatedTo(
        destination: Destination,
        pane: Pane,
        args: Map<String, String?> = emptyMap()
    ) {
        val last: NavigationIntent = emittedIntents.last()
        assertIs<NavigationIntent.NavigateTo>(last)
        assertThat(last.route).isEqualTo(destination(pane, args))
    }
}
