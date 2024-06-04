package com.stripe.android.identity.navigation

internal object ConsentDestination : IdentityTopLevelDestination() {
    const val CONSENT = "Consent"
    val ROUTE = object : DestinationRoute() {
        override val routeBase = CONSENT
    }

    override val destinationRoute = ROUTE
}
