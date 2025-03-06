package com.stripe.android.financialconnections.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.uicore.navigation.NavigationIntent
import com.stripe.android.uicore.navigation.NavigationManager
import com.stripe.android.uicore.navigation.PopUpToBehavior
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
        popUpTo: PopUpToBehavior?,
        isSingleTop: Boolean,
    ) {
        emittedIntents.add(
            NavigationIntent.NavigateTo(
                route = route,
                popUpTo = popUpTo,
                isSingleTop = isSingleTop,
            )
        )
    }

    override fun tryNavigateBack() {
        emittedIntents.add(NavigationIntent.NavigateBack)
    }

    fun assertNavigatedTo(
        destination: Destination,
        pane: Pane,
        popUpTo: PopUpToBehavior? = null,
    ) {
        val last: NavigationIntent = emittedIntents.last()
        assertIs<NavigationIntent.NavigateTo>(last)
        assertThat(last.route).isEqualTo(destination(pane))
        assertThat(last.popUpTo).isEqualTo(popUpTo)
    }

    fun assertNavigatedBack() {
        val last: NavigationIntent = emittedIntents.last()
        assertIs<NavigationIntent.NavigateBack>(last)
    }
}
