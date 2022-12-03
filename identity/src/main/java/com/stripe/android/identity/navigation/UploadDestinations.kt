package com.stripe.android.identity.navigation

import androidx.core.os.bundleOf
import com.stripe.android.identity.R

internal class PassportUploadDestination(
    shouldShowTakePhoto: Boolean,
    shouldShowChoosePhoto: Boolean,
    shouldPopUpToDocSelection: Boolean = false
) : IdentityTopLevelDestination() {
    override val destinationRoute = ROUTE
    override val destination =
        if (shouldPopUpToDocSelection) {
            R.id.action_global_passportUploadPopUpToDocSelect
        } else {
            R.id.action_global_passportUploadFragment
        }
    override val routeWithArgs = destinationRoute.withParams(
        ARG_SHOULD_SHOW_TAKE_PHOTO to shouldShowTakePhoto,
        ARG_SHOULD_SHOW_CHOOSE_PHOTO to shouldShowChoosePhoto
    )

    override val argsBundle = bundleOf(
        ARG_SHOULD_SHOW_TAKE_PHOTO to shouldShowTakePhoto,
        ARG_SHOULD_SHOW_CHOOSE_PHOTO to shouldShowChoosePhoto
    )

    companion object {
        const val PASSPORT_UPLOAD = "PassportUpload"
        val ROUTE = object : DestinationRoute() {
            override val routeBase = PASSPORT_UPLOAD
        }
    }
}

internal class IDUploadDestination(
    shouldShowTakePhoto: Boolean,
    shouldShowChoosePhoto: Boolean,
    shouldPopUpToDocSelection: Boolean = false
) : IdentityTopLevelDestination() {
    override val destinationRoute = ROUTE
    override val destination =
        if (shouldPopUpToDocSelection) {
            R.id.action_global_IDUploadPopUpToDocSelect
        } else {
            R.id.action_global_IDUploadFragment
        }
    override val routeWithArgs = destinationRoute.withParams(
        ARG_SHOULD_SHOW_TAKE_PHOTO to shouldShowTakePhoto,
        ARG_SHOULD_SHOW_CHOOSE_PHOTO to shouldShowChoosePhoto
    )

    override val argsBundle = bundleOf(
        ARG_SHOULD_SHOW_TAKE_PHOTO to shouldShowTakePhoto,
        ARG_SHOULD_SHOW_CHOOSE_PHOTO to shouldShowChoosePhoto
    )

    companion object {
        const val ID_UPLOAD = "IDUpload"
        val ROUTE = object : DestinationRoute() {
            override val routeBase = ID_UPLOAD
        }
    }
}

internal class DriveLicenseUploadDestination(
    shouldShowTakePhoto: Boolean,
    shouldShowChoosePhoto: Boolean,
    shouldPopUpToDocSelection: Boolean = false
) : IdentityTopLevelDestination() {
    override val destinationRoute = ROUTE
    override val destination =
        if (shouldPopUpToDocSelection) {
            R.id.action_global_driverLicenseUploadPopUpToDocSelect
        } else {
            R.id.action_global_driverLicenseUploadFragment
        }
    override val routeWithArgs = destinationRoute.withParams(
        ARG_SHOULD_SHOW_TAKE_PHOTO to shouldShowTakePhoto,
        ARG_SHOULD_SHOW_CHOOSE_PHOTO to shouldShowChoosePhoto
    )

    override val argsBundle = bundleOf(
        ARG_SHOULD_SHOW_TAKE_PHOTO to shouldShowTakePhoto,
        ARG_SHOULD_SHOW_CHOOSE_PHOTO to shouldShowChoosePhoto
    )

    companion object {
        const val DRIVE_LICENSE_UPLOAD = "DriverLicenseUpload"
        val ROUTE = object : DestinationRoute() {
            override val routeBase = DRIVE_LICENSE_UPLOAD
        }
    }
}

/**
 * Argument to indicate if choose photo option should be shown when picking an image.
 */
internal const val ARG_SHOULD_SHOW_CHOOSE_PHOTO = "shouldShowChoosePhoto"

/**
 * Argument to indicate if take photo option should be shown when picking an image.
 */
internal const val ARG_SHOULD_SHOW_TAKE_PHOTO = "shouldShowTakePhoto"
