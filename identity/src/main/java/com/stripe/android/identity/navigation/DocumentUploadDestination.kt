package com.stripe.android.identity.navigation

internal object DocumentUploadDestination : IdentityTopLevelDestination(
    popUpToParam = PopUpToParam(
        route = DocWarmupDestination.ROUTE.route,
        inclusive = false
    )
) {
    val ROUTE = object : DestinationRoute() {
        override val routeBase = UPLOAD
    }

    override val destinationRoute = ROUTE
}
