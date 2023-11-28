package com.stripe.android.identity.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleOwner
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

internal object SelfieDestination : IdentityTopLevelDestination(
    popUpToParam = PopUpToParam(
        route = SELFIE,
        inclusive = true
    )
) {
    val ROUTE = object : DestinationRoute() {
        override val routeBase = SELFIE
    }

    override val destinationRoute = ROUTE
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
