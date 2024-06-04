package com.stripe.android.identity.navigation

internal object IndividualDestination : IdentityTopLevelDestination(
    popUpToParam = PopUpToParam(
        route = INDIVIDUAL,
        inclusive = true
    )
) {
    val ROUTE = object : DestinationRoute() {
        override val routeBase = INDIVIDUAL
    }

    override val destinationRoute = ROUTE
}

internal const val INDIVIDUAL = "Individual"
