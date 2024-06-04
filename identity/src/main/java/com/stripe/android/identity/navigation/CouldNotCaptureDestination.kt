package com.stripe.android.identity.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument

internal class CouldNotCaptureDestination(
    fromSelfie: Boolean
) : IdentityTopLevelDestination() {
    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.withParams(
        ARG_FROM_SELFIE to fromSelfie
    )

    companion object {
        const val COULD_NOT_CAPTURE = "CouldNotCapture"
        const val ARG_FROM_SELFIE = "fromSelfie"

        val ROUTE = object : DestinationRoute() {
            override val routeBase = COULD_NOT_CAPTURE

            override val arguments = listOf(
                navArgument(ARG_FROM_SELFIE) {
                    type = NavType.BoolType
                }
            )
        }

        fun fromSelfie(backStackEntry: NavBackStackEntry) =
            backStackEntry.getBooleanArgument(ARG_FROM_SELFIE)
    }
}
