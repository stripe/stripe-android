package com.stripe.android.identity.navigation

internal object OTPDestination : IdentityTopLevelDestination() {
    const val OTP = "OTP"

    val ROUTE = object : DestinationRoute() {
        override val routeBase = OTP
    }
    override val destinationRoute = ROUTE
}
