package com.stripe.android.financialconnections.navigation

import java.util.UUID

/**
 * State that can be used to trigger navigation.
 */
internal sealed class NavigationState {

    object Idle : NavigationState()

    data class NavigateToRoute(
        val command: NavigationCommand,
        val popCurrentFromBackStack: Boolean = false,
        val id: String = UUID.randomUUID().toString()
    ) : NavigationState()
}
