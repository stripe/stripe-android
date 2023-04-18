package com.stripe.android.identity.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.IdentityScanViewModel

internal abstract class DocumentScanDestination(
    shouldStartFromBack: Boolean = false,
    shouldPopUpToDocSelection: Boolean = false,
    frontScanType: IdentityScanState.ScanType,
    backScanType: IdentityScanState.ScanType,
    @StringRes frontTitleStringRes: Int,
    @StringRes backTitleStringRes: Int = INVALID_STRING_RES,
    @StringRes frontMessageStringRes: Int,
    @StringRes backMessageStringRes: Int = INVALID_STRING_RES,
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
            ARG_SHOULD_START_FROM_BACK to shouldStartFromBack,
            ARG_FRONT_SCAN_TYPE to frontScanType,
            ARG_BACK_SCAN_TYPE to backScanType,
            ARG_FRONT_TITLE_STRING_RES to frontTitleStringRes,
            ARG_BACK_TITLE_STRING_RES to backTitleStringRes,
            ARG_FRONT_MESSAGE_STRING_RES to frontMessageStringRes,
            ARG_BACK_MESSAGE_STRING_RES to backMessageStringRes,
            ARG_COLLECTED_DATA_PARAM_TYPE to collectedDataParamType
        )
    }

    internal companion object {
        fun shouldStartFromBack(backStackEntry: NavBackStackEntry) =
            backStackEntry.getBooleanArgument(ARG_SHOULD_START_FROM_BACK)

        fun frontScanType(backStackEntry: NavBackStackEntry) =
            backStackEntry.arguments?.getSerializable(ARG_FRONT_SCAN_TYPE) as IdentityScanState.ScanType

        fun backScanType(backStackEntry: NavBackStackEntry) =
            backStackEntry.arguments?.getSerializable(ARG_BACK_SCAN_TYPE) as? IdentityScanState.ScanType

        fun frontTitleStringRes(backStackEntry: NavBackStackEntry) =
            backStackEntry.getIntArgument(ARG_FRONT_TITLE_STRING_RES)

        fun backTitleStringRes(backStackEntry: NavBackStackEntry) =
            backStackEntry.getIntArgument(ARG_BACK_TITLE_STRING_RES)

        fun frontMessageStringRes(backStackEntry: NavBackStackEntry) =
            backStackEntry.getIntArgument(ARG_FRONT_MESSAGE_STRING_RES)

        fun backMessageStringRes(backStackEntry: NavBackStackEntry) =
            backStackEntry.getIntArgument(ARG_BACK_MESSAGE_STRING_RES)

        fun collectedDataParamType(backStackEntry: NavBackStackEntry) =
            backStackEntry.arguments?.getSerializable(ARG_COLLECTED_DATA_PARAM_TYPE) as CollectedDataParam.Type
    }
}

internal class DocumentScanDestinationRoute(
    override val routeBase: String
) :
    IdentityTopLevelDestination.DestinationRoute() {
    override val arguments = mutableListOf(
        navArgument(ARG_SHOULD_START_FROM_BACK) {
            type = NavType.BoolType
        },
        navArgument(ARG_FRONT_SCAN_TYPE) {
            type = NavType.EnumType(IdentityScanState.ScanType::class.java)
        },
        navArgument(ARG_BACK_SCAN_TYPE) {
            type = NavType.EnumType(IdentityScanState.ScanType::class.java)
        },
        navArgument(ARG_FRONT_TITLE_STRING_RES) {
            type = NavType.IntType
        },
        navArgument(ARG_BACK_TITLE_STRING_RES) {
            type = NavType.IntType
        },
        navArgument(ARG_FRONT_MESSAGE_STRING_RES) {
            type = NavType.IntType
        },
        navArgument(ARG_BACK_MESSAGE_STRING_RES) {
            type = NavType.IntType
        },
        navArgument(ARG_COLLECTED_DATA_PARAM_TYPE) {
            type = NavType.EnumType(CollectedDataParam.Type::class.java)
        }
    )
}

internal class PassportScanDestination(
    shouldStartFromBack: Boolean = false,
    shouldPopUpToDocSelection: Boolean = false
) : DocumentScanDestination(
    shouldStartFromBack = shouldStartFromBack,
    shouldPopUpToDocSelection = shouldPopUpToDocSelection,
    frontScanType = IdentityScanState.ScanType.PASSPORT,
    backScanType = IdentityScanState.ScanType.PASSPORT,
    frontTitleStringRes = R.string.passport,
    frontMessageStringRes = R.string.position_passport,
    collectedDataParamType = CollectedDataParam.Type.PASSPORT
) {
    override val destinationRoute = ROUTE

    companion object {
        private const val PASSPORT_SCAN = "PassportScan"
        val ROUTE = DocumentScanDestinationRoute(routeBase = PASSPORT_SCAN)
    }
}

internal class IDScanDestination(
    shouldStartFromBack: Boolean = false,
    shouldPopUpToDocSelection: Boolean = false
) : DocumentScanDestination(
    shouldStartFromBack = shouldStartFromBack,
    shouldPopUpToDocSelection = shouldPopUpToDocSelection,
    frontScanType = IdentityScanState.ScanType.ID_FRONT,
    backScanType = IdentityScanState.ScanType.ID_BACK,
    frontTitleStringRes = R.string.front_of_id,
    backTitleStringRes = R.string.back_of_id,
    frontMessageStringRes = R.string.position_id_front,
    backMessageStringRes = R.string.position_id_back,
    collectedDataParamType = CollectedDataParam.Type.IDCARD
) {
    override val destinationRoute = ROUTE

    companion object {
        private const val ID_SCAN = "IDScan"
        val ROUTE = DocumentScanDestinationRoute(routeBase = ID_SCAN)
    }
}

internal class DriverLicenseScanDestination(
    shouldStartFromBack: Boolean = false,
    shouldPopUpToDocSelection: Boolean = false,
) : DocumentScanDestination(
    shouldStartFromBack = shouldStartFromBack,
    shouldPopUpToDocSelection = shouldPopUpToDocSelection,
    frontScanType = IdentityScanState.ScanType.DL_FRONT,
    backScanType = IdentityScanState.ScanType.DL_BACK,
    frontTitleStringRes = R.string.front_of_dl,
    backTitleStringRes = R.string.back_of_dl,
    frontMessageStringRes = R.string.position_dl_front,
    backMessageStringRes = R.string.position_dl_back,
    collectedDataParamType = CollectedDataParam.Type.DRIVINGLICENSE
) {
    override val destinationRoute = ROUTE

    companion object {
        private const val DRIVE_LICENSE_SCAN = "DriverLicenseScan"
        val ROUTE = DocumentScanDestinationRoute(routeBase = DRIVE_LICENSE_SCAN)
    }
}

@Composable
internal fun ScanDestinationEffect(
    lifecycleOwner: LifecycleOwner,
    identityScanViewModel: IdentityScanViewModel
) {
    DisposableEffect(Unit) {
        onDispose {
            identityScanViewModel.clearDisplayStateChangedFlow()
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

internal const val SELFIE = "Selfie"
internal const val ARG_SHOULD_START_FROM_BACK = "startFromBack"
internal const val ARG_FRONT_SCAN_TYPE = "frontScanType"
internal const val ARG_BACK_SCAN_TYPE = "backScanType"
internal const val ARG_FRONT_TITLE_STRING_RES = "frontTitleStringRes"
internal const val ARG_BACK_TITLE_STRING_RES = "backTitleStringRes"
internal const val ARG_FRONT_MESSAGE_STRING_RES = "frontMessageStringRes"
internal const val ARG_BACK_MESSAGE_STRING_RES = "backMessageStringRes"
internal const val ARG_COLLECTED_DATA_PARAM_TYPE = "collectedDataParamType"
internal const val INVALID_STRING_RES = -1
