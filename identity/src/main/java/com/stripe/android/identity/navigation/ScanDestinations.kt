package com.stripe.android.identity.navigation

import androidx.core.os.bundleOf
import com.stripe.android.identity.R

internal class PassportScanDestination(
    shouldStartFromBack: Boolean = false,
    shouldPopUpToDocSelection: Boolean = false
) : IdentityTopLevelDestination() {
    override val destinationRoute = ROUTE
    override val destination =
        if (shouldPopUpToDocSelection) {
            R.id.action_global_passportScanPopUpToDocSelect
        } else {
            R.id.action_global_passportScanFragment
        }

    override val routeWithArgs = destinationRoute.withParams(
        ARG_SHOULD_START_FROM_BACK to shouldStartFromBack
    )

    override val argsBundle = bundleOf(
        ARG_SHOULD_START_FROM_BACK to shouldStartFromBack
    )

    companion object {
        const val PASSPORT_SCAN = "PassportScan"
        val ROUTE = object : DestinationRoute() {
            override val routeBase = PASSPORT_SCAN
        }
    }
}

internal class IDScanDestination(
    shouldStartFromBack: Boolean = false,
    shouldPopUpToDocSelection: Boolean = false
) : IdentityTopLevelDestination() {
    override val destinationRoute = ROUTE
    override val destination =
        if (shouldPopUpToDocSelection) {
            R.id.action_global_IDScanPopUpToDocSelect
        } else {
            R.id.action_global_IDScanFragment
        }
    override val routeWithArgs = destinationRoute.withParams(
        ARG_SHOULD_START_FROM_BACK to shouldStartFromBack
    )

    override val argsBundle = bundleOf(
        ARG_SHOULD_START_FROM_BACK to shouldStartFromBack
    )

    companion object {
        const val ID_SCAN = "IDScan"
        val ROUTE = object : DestinationRoute() {
            override val routeBase = ID_SCAN
        }
    }
}

internal class DriverLicenseScanDestination(
    shouldStartFromBack: Boolean = false,
    shouldPopUpToDocSelection: Boolean = false
) : IdentityTopLevelDestination() {
    override val destinationRoute = ROUTE
    override val destination =
        if (shouldPopUpToDocSelection) {
            R.id.action_global_driverLicenseScanPopUpToDocSelect
        } else {
            R.id.action_global_driverLicenseScanFragment
        }
    override val routeWithArgs = destinationRoute.withParams(
        ARG_SHOULD_START_FROM_BACK to shouldStartFromBack
    )

    override val argsBundle = bundleOf(
        ARG_SHOULD_START_FROM_BACK to shouldStartFromBack
    )

    companion object {
        const val DRIVE_LICENSE_SCAN = "DriverLicenseScan"
        val ROUTE = object : DestinationRoute() {
            override val routeBase = DRIVE_LICENSE_SCAN
        }
    }
}

internal object SelfieDestination : IdentityTopLevelDestination() {
    const val SELFIE = "Selfie"
    private val ROUTE = object : DestinationRoute() {
        override val routeBase = SELFIE
    }
    override val destination = R.id.action_global_selfieFragment
    override val destinationRoute = ROUTE
    override val routeWithArgs = destinationRoute.route
}

internal const val ARG_SHOULD_START_FROM_BACK = "startFromBack"
