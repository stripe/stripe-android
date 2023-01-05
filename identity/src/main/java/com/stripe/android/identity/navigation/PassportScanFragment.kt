package com.stripe.android.identity.navigation

import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState

/**
 * Fragment to scan passport.
 */
internal class PassportScanFragment(
    identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityDocumentScanFragment(
    identityCameraScanViewModelFactory,
    identityViewModelFactory
) {
    override val frontScanType = IdentityScanState.ScanType.PASSPORT
    override val backScanType: IdentityScanState.ScanType? = null
    override val frontTitleStringRes = R.string.passport
    override val backTitleStringRes = INVALID
    override val frontMessageStringRes = R.string.position_passport
    override val backMessageStringRes = INVALID
    override val collectedDataParamType = CollectedDataParam.Type.PASSPORT
    override val destinationRoute = PassportScanDestination.ROUTE

    internal companion object {
        const val INVALID = -1
    }
}
