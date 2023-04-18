package com.stripe.android.identity.navigation

internal object IndividualWelcomeDestination : IdentityTopLevelDestination() {

    const val INDIVIDUAL_WELCOME = "IndividualWelcome"

    val ROUTE = object : DestinationRoute() {
        override val routeBase = INDIVIDUAL_WELCOME
    }

    override val destinationRoute = ROUTE
}
