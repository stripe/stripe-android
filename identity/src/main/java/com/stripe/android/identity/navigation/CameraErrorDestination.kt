package com.stripe.android.identity.navigation

internal object CameraErrorDestination : IdentityTopLevelDestination(
    popUpToParam = PopUpToParam(
        route = DocWarmupDestination.ROUTE.route,
        inclusive = false
    )
) {
    val ROUTE = object : DestinationRoute() {
        override val routeBase = "CameraError"
    }

    override val destinationRoute = ROUTE
}
