package com.stripe.android.identity.navigation

internal object SelfieWarmupDestination : IdentityTopLevelDestination(
    popUpToParam = PopUpToParam(
        route = DocSelectionDestination.ROUTE.route,
        inclusive = false
    )
) {
    const val SELFIE_WARMUP = "SelfieWarmup"
    val ROUTE = object : DestinationRoute() {
        override val routeBase = SELFIE_WARMUP
    }

    override val destinationRoute = ROUTE
}
