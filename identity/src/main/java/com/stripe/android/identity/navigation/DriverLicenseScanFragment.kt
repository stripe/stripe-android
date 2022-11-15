package com.stripe.android.identity.navigation

import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState.ScanType.DL_BACK
import com.stripe.android.identity.states.IdentityScanState.ScanType.DL_FRONT

/**
 * Fragment to scan the Driver's license.
 */
internal class DriverLicenseScanFragment(
    identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityDocumentScanFragment(
    identityCameraScanViewModelFactory,
    identityViewModelFactory
) {
    override val frontScanType = DL_FRONT
    override val backScanType = DL_BACK
    override val fragmentId = R.id.driverLicenseScanFragment
    override val frontTitleStringRes = R.string.front_of_dl
    override val backTitleStringRes = R.string.back_of_dl
    override val frontMessageStringRes = R.string.position_dl_front
    override val backMessageStringRes = R.string.position_dl_back
    override val collectedDataParamType = CollectedDataParam.Type.DRIVINGLICENSE
}
