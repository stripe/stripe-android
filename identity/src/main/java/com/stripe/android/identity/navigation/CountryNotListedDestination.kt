package com.stripe.android.identity.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument

internal class CountryNotListedDestination(
    isMissingId: Boolean,
    isFromStandaloneIndividual: Boolean
) : IdentityTopLevelDestination() {

    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.withParams(
        ARG_IS_MISSING_ID to isMissingId,
        ARG_IS_FROM_STANDALONE_INDIVIDUAL to isFromStandaloneIndividual
    )

    internal companion object {
        const val COUNTRY_NOT_LISTED = "CountryNotListed"
        const val ARG_IS_MISSING_ID = "isMissingId"
        const val ARG_IS_FROM_STANDALONE_INDIVIDUAL = "isFromStandaloneIndividual"

        fun isMissingId(backStackEntry: NavBackStackEntry) =
            backStackEntry.getBooleanArgument(ARG_IS_MISSING_ID)

        fun isFromStandaloneIndividual(backStackEntry: NavBackStackEntry) =
            backStackEntry.getBooleanArgument(ARG_IS_FROM_STANDALONE_INDIVIDUAL)

        val ROUTE = object : DestinationRoute() {
            override val routeBase = COUNTRY_NOT_LISTED
            override val arguments = listOf(
                navArgument(ARG_IS_MISSING_ID) {
                    type = NavType.BoolType
                },
                navArgument(ARG_IS_FROM_STANDALONE_INDIVIDUAL) {
                    type = NavType.BoolType
                },
            )
        }
    }
}
