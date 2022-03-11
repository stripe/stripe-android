package com.stripe.android.identity.navigation

import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.states.IdentityScanState

/**
 * Fragment to upload Driver license.
 */
internal class DriverLicenseUploadFragment(
    frontBackUploadViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : FrontBackUploadFragment(frontBackUploadViewModelFactory, identityViewModelFactory) {
    override val titleRes = R.string.file_upload
    override val contextRes = R.string.file_upload_content_dl
    override val frontTextRes = R.string.front_of_dl
    override val backTextRes = R.string.back_of_dl
    override val frontCheckMarkContentDescription = R.string.front_of_dl_selected
    override val backCheckMarkContentDescription = R.string.back_of_dl_selected
    override val continueButtonNavigationId =
        R.id.action_driverLicenseUploadFragment_to_confirmationFragment
    override val frontScanType = IdentityScanState.ScanType.DL_FRONT
    override val backScanType = IdentityScanState.ScanType.DL_BACK
}
