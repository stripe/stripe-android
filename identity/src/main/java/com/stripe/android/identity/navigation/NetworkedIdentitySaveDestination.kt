package com.stripe.android.identity.navigation

internal object NetworkedIdentitySaveDestination : IdentityTopLevelDestination() {
    const val NETWORKED_IDENTITY_SAVE = "NetworkedIdentitySave"
    val ROUTE = object : DestinationRoute() {
        override val routeBase = NETWORKED_IDENTITY_SAVE
    }

    override val destinationRoute = ROUTE
}
