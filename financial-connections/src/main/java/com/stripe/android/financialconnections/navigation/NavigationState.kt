package com.stripe.android.financialconnections.navigation

import java.util.UUID

/**
 * State that can be used to trigger navigation.
 */
internal sealed class NavigationState {

    object Idle : NavigationState()

    data class NavigateToRoute(
        val command: NavigationCommand,
        val id: String = UUID.randomUUID().toString()
    ) :
        NavigationState()

    data class PopToRoute(
        val command: NavigationCommand,
        val inclusive: Boolean = false,
        val id: String = UUID.randomUUID().toString()
    ) :
        NavigationState()
}
