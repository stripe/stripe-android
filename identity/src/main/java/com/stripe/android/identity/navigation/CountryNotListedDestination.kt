package com.stripe.android.identity.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument

internal class CountryNotListedDestination(
    isMissingId: Boolean
) : IdentityTopLevelDestination() {

    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.withParams(
        ARG_IS_MISSING_ID to isMissingId
    )

    internal companion object {
        const val COUNTRY_NOT_LISTED = "CountryNotListed"
        const val ARG_IS_MISSING_ID = "isMissingId"
        fun isMissingId(backStackEntry: NavBackStackEntry) =
            backStackEntry.getBooleanArgument(ARG_IS_MISSING_ID)

        val ROUTE = object : DestinationRoute() {
            override val routeBase = COUNTRY_NOT_LISTED
            override val arguments = listOf(
                navArgument(ARG_IS_MISSING_ID) {
                    type = NavType.BoolType
                }
            )
        }
    }
}
