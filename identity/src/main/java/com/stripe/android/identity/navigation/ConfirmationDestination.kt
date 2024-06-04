package com.stripe.android.identity.navigation

internal object ConfirmationDestination : IdentityTopLevelDestination() {
    const val CONFIRMATION = "Confirmation"
    val ROUTE = object : DestinationRoute() {
        override val routeBase = CONFIRMATION
    }

    override val destinationRoute = ROUTE
}
