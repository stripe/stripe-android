package com.stripe.android.identity.navigation

internal object InitialLoadingDestination : IdentityTopLevelDestination() {
    private const val LOADING = "Loading"
    val ROUTE = object : DestinationRoute() {
        override val routeBase = LOADING
    }
    override val destinationRoute = ROUTE
}
