package com.stripe.android.identity.navigation

internal object DebugDestination : IdentityTopLevelDestination() {
    const val DEBUG = "Debug"
    val ROUTE = object : DestinationRoute() {
        override val routeBase = DEBUG
    }

    override val destinationRoute = ROUTE
}
