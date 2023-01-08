package com.stripe.android.identity.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.stripe.android.identity.networking.models.CollectedDataParam

internal class CameraPermissionDeniedDestination(
    collectedDataParamType: CollectedDataParam.Type = CollectedDataParam.Type.INVALID,
) : IdentityTopLevelDestination() {
    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.withParams(
        ARG_COLLECTED_DATA_PARAM_TYPE to collectedDataParamType
    )

    companion object {
        const val CAMERA_PERMISSION_DENIED = "CameraPermissionDenied"
        const val ARG_COLLECTED_DATA_PARAM_TYPE = "collectedDataParamType"

        val ROUTE = object : DestinationRoute() {
            override val routeBase = CAMERA_PERMISSION_DENIED
            override val arguments = listOf(
                navArgument(ARG_COLLECTED_DATA_PARAM_TYPE) {
                    type = NavType.EnumType(CollectedDataParam.Type::class.java)
                }
            )
        }

        fun collectedDataParamType(backStackEntry: NavBackStackEntry) =
            backStackEntry.arguments?.getSerializable(ARG_COLLECTED_DATA_PARAM_TYPE) as CollectedDataParam.Type
    }
}
