package com.stripe.android.identity.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.stripe.android.identity.states.IdentityScanState

internal class CouldNotCaptureDestination(
    scanType: IdentityScanState.ScanType,
    requireLiveCapture: Boolean
) : IdentityTopLevelDestination() {
    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.withParams(
        ARG_COULD_NOT_CAPTURE_SCAN_TYPE to scanType,
        ARG_REQUIRE_LIVE_CAPTURE to requireLiveCapture
    )

    companion object {
        const val COULD_NOT_CAPTURE = "CouldNotCapture"
        const val ARG_COULD_NOT_CAPTURE_SCAN_TYPE = "scanType"
        const val ARG_REQUIRE_LIVE_CAPTURE = "requireLiveCapture"

        val ROUTE = object : DestinationRoute() {
            override val routeBase = COULD_NOT_CAPTURE

            override val arguments = listOf(
                navArgument(ARG_COULD_NOT_CAPTURE_SCAN_TYPE) {
                    type = NavType.EnumType(IdentityScanState.ScanType::class.java)
                },
                navArgument(ARG_REQUIRE_LIVE_CAPTURE) {
                    type = NavType.BoolType
                }
            )
        }

        fun couldNotCaptureScanType(backStackEntry: NavBackStackEntry) =
            backStackEntry.arguments?.getSerializable(
                ARG_COULD_NOT_CAPTURE_SCAN_TYPE
            ) as IdentityScanState.ScanType

        fun requireLiveCapture(backStackEntry: NavBackStackEntry) =
            backStackEntry.getBooleanArgument(ARG_REQUIRE_LIVE_CAPTURE)
    }
}
