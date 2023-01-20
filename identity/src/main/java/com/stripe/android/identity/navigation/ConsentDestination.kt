package com.stripe.android.identity.navigation

internal object ConsentDestination : IdentityTopLevelDestination() {
    private const val CONSENT = "Consent"
    val ROUTE = object : DestinationRoute() {
        override val routeBase = CONSENT
    }

    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.route
}
