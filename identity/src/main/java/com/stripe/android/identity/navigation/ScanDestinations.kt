package com.stripe.android.identity.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.stripe.android.identity.viewmodel.IdentityScanViewModel

@Composable
internal fun ScanDestinationEffect(
    lifecycleOwner: LifecycleOwner,
    identityScanViewModel: IdentityScanViewModel
) {
    DisposableEffect(Unit) {
        onDispose {
            identityScanViewModel.stopScan(lifecycleOwner)
        }
    }
}

internal class SelfieDestination(
    trainingConsent: Boolean
) : IdentityTopLevelDestination(
    popUpToParam = PopUpToParam(
        route = SELFIE,
        inclusive = true
    )
) {
    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.withParams(
        ARG_TRAINING_CONSENT to trainingConsent
    )

    companion object {
        const val ARG_TRAINING_CONSENT = "trainingConsent"

        val ROUTE = object : DestinationRoute() {
            override val routeBase = SELFIE
            override val arguments = listOf(
                navArgument(ARG_TRAINING_CONSENT) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        }

        fun trainingConsent(backStackEntry: NavBackStackEntry) =
            backStackEntry.getBooleanArgument(ARG_TRAINING_CONSENT)
    }
}

internal object DocumentScanDestination : IdentityTopLevelDestination(
    popUpToParam = PopUpToParam(
        route = DocWarmupDestination.ROUTE.route,
        inclusive = false
    )
) {
    val ROUTE = object : DestinationRoute() {
        override val routeBase = SCAN
    }

    override val destinationRoute = ROUTE
}

internal const val SELFIE = "Selfie"
internal const val SCAN = "Scan"
internal const val UPLOAD = "Upload"
