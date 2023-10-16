package com.stripe.android.identity.navigation

import androidx.annotation.StringRes
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState

internal abstract class DocumentUploadDestination(
    shouldPopUpToDocSelection: Boolean,
    frontScanType: IdentityScanState.ScanType,
    backScanType: IdentityScanState.ScanType,
    @StringRes titleRes: Int,
    @StringRes contextRes: Int,
    @StringRes frontDescriptionRes: Int,
    @StringRes frontCheckMarkDescriptionRes: Int,
    @StringRes backDescriptionRes: Int = INVALID_STRING_RES,
    @StringRes backCheckMarkDescriptionRes: Int = INVALID_STRING_RES,
    collectedDataParamType: CollectedDataParam.Type
) : IdentityTopLevelDestination(
    popUpToParam = if (shouldPopUpToDocSelection) {
        PopUpToParam(
            route = DocSelectionDestination.ROUTE.route,
            inclusive = false
        )
    } else {
        null
    }
) {
    override val routeWithArgs by lazy {
        destinationRoute.withParams(
            ARG_FRONT_SCAN_TYPE to frontScanType,
            ARG_BACK_SCAN_TYPE to backScanType,
            ARG_TITLE_RES to titleRes,
            ARG_CONTEXT_RES to contextRes,
            ARG_FRONT_DESCRIPTION_RES to frontDescriptionRes,
            ARG_FRONT_CHECK_MARK_DESCRIPTION_RES to frontCheckMarkDescriptionRes,
            ARG_BACK_DESCRIPTION_RES to backDescriptionRes,
            ARG_BACK_CHECK_MARK_DESCRIPTION_RES to backCheckMarkDescriptionRes,
            ARG_COLLECTED_DATA_PARAM_TYPE to collectedDataParamType
        )
    }

    companion object {
        const val PASSPORT_UPLOAD = "PassportUpload"
        val ROUTE = object : DestinationRoute() {
            override val routeBase = PASSPORT_UPLOAD
        }

        fun frontScanType(backStackEntry: NavBackStackEntry) =
            backStackEntry.arguments?.getSerializable(ARG_FRONT_SCAN_TYPE) as IdentityScanState.ScanType

        fun backScanType(backStackEntry: NavBackStackEntry) =
            backStackEntry.arguments?.getSerializable(ARG_BACK_SCAN_TYPE) as IdentityScanState.ScanType

        fun titleRes(backStackEntry: NavBackStackEntry) =
            backStackEntry.getIntArgument(ARG_TITLE_RES)

        fun contextRes(backStackEntry: NavBackStackEntry) =
            backStackEntry.getIntArgument(ARG_CONTEXT_RES)

        fun frontDescriptionRes(backStackEntry: NavBackStackEntry) =
            backStackEntry.getIntArgument(ARG_FRONT_DESCRIPTION_RES)

        fun frontCheckMarkDescriptionRes(backStackEntry: NavBackStackEntry) =
            backStackEntry.getIntArgument(ARG_FRONT_CHECK_MARK_DESCRIPTION_RES)

        fun backDescriptionRes(backStackEntry: NavBackStackEntry) =
            backStackEntry.getIntArgument(ARG_BACK_DESCRIPTION_RES)

        fun backCheckMarkDescriptionRes(backStackEntry: NavBackStackEntry) =
            backStackEntry.getIntArgument(ARG_BACK_CHECK_MARK_DESCRIPTION_RES)

        fun collectedDataParamType(backStackEntry: NavBackStackEntry) =
            backStackEntry.arguments?.getSerializable(ARG_COLLECTED_DATA_PARAM_TYPE) as CollectedDataParam.Type
    }
}

internal class DocumentUploadDestinationRoute(
    override val routeBase: String
) : IdentityTopLevelDestination.DestinationRoute() {
    override val arguments = mutableListOf(
        navArgument(ARG_FRONT_SCAN_TYPE) {
            type = NavType.EnumType(IdentityScanState.ScanType::class.java)
        },
        navArgument(ARG_BACK_SCAN_TYPE) {
            type = NavType.EnumType(IdentityScanState.ScanType::class.java)
        },
        navArgument(ARG_TITLE_RES) {
            type = NavType.IntType
        },
        navArgument(ARG_CONTEXT_RES) {
            type = NavType.IntType
        },
        navArgument(ARG_FRONT_DESCRIPTION_RES) {
            type = NavType.IntType
        },
        navArgument(ARG_FRONT_CHECK_MARK_DESCRIPTION_RES) {
            type = NavType.IntType
        },
        navArgument(ARG_BACK_DESCRIPTION_RES) {
            type = NavType.IntType
        },
        navArgument(ARG_BACK_CHECK_MARK_DESCRIPTION_RES) {
            type = NavType.IntType
        },
        navArgument(ARG_COLLECTED_DATA_PARAM_TYPE) {
            type = NavType.EnumType(CollectedDataParam.Type::class.java)
        }
    )
}

internal class PassportUploadDestination(
    shouldPopUpToDocSelection: Boolean = false,
) : DocumentUploadDestination(
    shouldPopUpToDocSelection = shouldPopUpToDocSelection,
    frontScanType = IdentityScanState.ScanType.PASSPORT,
    backScanType = IdentityScanState.ScanType.PASSPORT,
    titleRes = R.string.stripe_upload_your_photo_id,
    contextRes = R.string.stripe_file_upload_content_passport,
    frontDescriptionRes = R.string.stripe_passport,
    frontCheckMarkDescriptionRes = R.string.stripe_passport_selected,
    collectedDataParamType = CollectedDataParam.Type.PASSPORT
) {
    override val destinationRoute = ROUTE

    companion object {
        private const val PASSPORT_UPLOAD = "PassportUpload"
        val ROUTE = DocumentUploadDestinationRoute(routeBase = PASSPORT_UPLOAD)
    }
}

internal class IDUploadDestination(
    shouldPopUpToDocSelection: Boolean = false
) : DocumentUploadDestination(
    shouldPopUpToDocSelection = shouldPopUpToDocSelection,
    frontScanType = IdentityScanState.ScanType.ID_FRONT,
    backScanType = IdentityScanState.ScanType.ID_BACK,
    titleRes = R.string.stripe_upload_your_photo_id,
    contextRes = R.string.stripe_file_upload_content_id,
    frontDescriptionRes = R.string.stripe_front_of_id,
    frontCheckMarkDescriptionRes = R.string.stripe_front_of_id_selected,
    backDescriptionRes = R.string.stripe_back_of_id,
    backCheckMarkDescriptionRes = R.string.stripe_back_of_id_selected,
    collectedDataParamType = CollectedDataParam.Type.IDCARD
) {
    override val destinationRoute = ROUTE

    companion object {
        private const val ID_UPLOAD = "IDUpload"
        val ROUTE = DocumentUploadDestinationRoute(routeBase = ID_UPLOAD)
    }
}

internal class DriverLicenseUploadDestination(
    shouldPopUpToDocSelection: Boolean = false
) : DocumentUploadDestination(
    shouldPopUpToDocSelection = shouldPopUpToDocSelection,
    frontScanType = IdentityScanState.ScanType.DL_FRONT,
    backScanType = IdentityScanState.ScanType.DL_BACK,
    titleRes = R.string.stripe_upload_your_photo_id,
    contextRes = R.string.stripe_file_upload_content_dl,
    frontDescriptionRes = R.string.stripe_front_of_dl,
    frontCheckMarkDescriptionRes = R.string.stripe_front_of_dl_selected,
    backDescriptionRes = R.string.stripe_back_of_dl,
    backCheckMarkDescriptionRes = R.string.stripe_back_of_dl_selected,
    collectedDataParamType = CollectedDataParam.Type.DRIVINGLICENSE
) {
    override val destinationRoute = ROUTE

    companion object {
        private const val DRIVE_LICENSE_UPLOAD = "DriverLicenseUpload"
        val ROUTE = DocumentUploadDestinationRoute(routeBase = DRIVE_LICENSE_UPLOAD)
    }
}

internal const val ARG_TITLE_RES = "titleRes"
internal const val ARG_CONTEXT_RES = "contextRes"
internal const val ARG_FRONT_DESCRIPTION_RES = "frontDescriptionRes"
internal const val ARG_FRONT_CHECK_MARK_DESCRIPTION_RES = "frontCheckMarkDescriptionRes"
internal const val ARG_BACK_DESCRIPTION_RES = "backDescriptionRes"
internal const val ARG_BACK_CHECK_MARK_DESCRIPTION_RES = "backCheckMarkDescriptionRes"
