package com.stripe.android.identity.navigation

internal object IndividualDestination : IdentityTopLevelDestination() {
    const val INDIVIDUAL = "Individual"

    val ROUTE = object : DestinationRoute() {
        override val routeBase = INDIVIDUAL
    }

    override val destinationRoute = ROUTE
}
