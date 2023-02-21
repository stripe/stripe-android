package com.stripe.android.identity.navigation

internal object DocSelectionDestination : IdentityTopLevelDestination() {
    private const val DOC_SELECTION = "DocSelection"
    val ROUTE = object : DestinationRoute() {
        override val routeBase = DOC_SELECTION
    }

    override val destinationRoute = ROUTE
}
