package com.stripe.android.identity.navigation

internal object CameraPermissionDeniedDestination : IdentityTopLevelDestination() {
    const val CAMERA_PERMISSION_DENIED = "CameraPermissionDenied"
    val ROUTE = object : DestinationRoute() {
        override val routeBase = CAMERA_PERMISSION_DENIED
    }
    override val destinationRoute = ROUTE
}
