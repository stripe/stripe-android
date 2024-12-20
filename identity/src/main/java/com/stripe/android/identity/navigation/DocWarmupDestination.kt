package com.stripe.android.identity.navigation

internal object DocWarmupDestination : IdentityTopLevelDestination(
    popUpToParam = PopUpToParam(
        route = DOC_WARMUP,
        inclusive = true
    )
) {
    val ROUTE = object : DestinationRoute() {
        override val routeBase = DOC_WARMUP
    }

    override val destinationRoute = ROUTE
}

private const val DOC_WARMUP = "DocWarmup"
