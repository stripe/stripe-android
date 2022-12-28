package com.stripe.android.identity.navigation

import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState.ScanType.ID_BACK
import com.stripe.android.identity.states.IdentityScanState.ScanType.ID_FRONT

/**
 * Fragment to scan the ID.
 */
internal class IDScanFragment(
    identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityDocumentScanFragment(
    identityCameraScanViewModelFactory,
    identityViewModelFactory
) {
    override val frontScanType = ID_FRONT
    override val backScanType = ID_BACK
    override val fragmentId = R.id.IDScanFragment
    override val frontTitleStringRes = R.string.front_of_id
    override val backTitleStringRes = R.string.back_of_id
    override val frontMessageStringRes = R.string.position_id_front
    override val backMessageStringRes = R.string.position_id_back
    override val collectedDataParamType = CollectedDataParam.Type.IDCARD
    override val route = IDScanDestination.ROUTE.route
}
