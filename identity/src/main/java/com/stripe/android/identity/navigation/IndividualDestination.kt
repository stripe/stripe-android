package com.stripe.android.identity.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument

internal class IndividualDestination(
    standalone: Boolean
) : IdentityTopLevelDestination() {

    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.withParams(
        ARG_STANDALONE to standalone
    )

    internal companion object {
        const val INDIVIDUAL = "Individual"

        // Indicates if it's the only screen required to collect user information.
        const val ARG_STANDALONE = "standAlone"

        val ROUTE = object : DestinationRoute() {
            override val routeBase = INDIVIDUAL
            override val arguments = listOf(
                navArgument(ARG_STANDALONE) {
                    type = NavType.BoolType
                }
            )
        }

        fun isStandAlone(backStackEntry: NavBackStackEntry) =
            backStackEntry.getBooleanArgument(ARG_STANDALONE)
    }
}
