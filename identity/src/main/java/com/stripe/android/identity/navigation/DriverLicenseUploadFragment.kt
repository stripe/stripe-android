package com.stripe.android.identity.navigation

import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.IdentityIO

/**
 * Fragment to upload Driver license.
 */
internal class DriverLicenseUploadFragment(
    identityIO: IdentityIO,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityUploadFragment(identityIO, identityViewModelFactory) {
    override val titleRes = R.string.file_upload
    override val contextRes = R.string.file_upload_content_dl
    override val frontTextRes = R.string.front_of_dl
    override var backTextRes: Int? = R.string.back_of_dl
    override val frontCheckMarkContentDescription = R.string.front_of_dl_selected
    override var backCheckMarkContentDescription: Int? = R.string.back_of_dl_selected
    override val frontScanType = IdentityScanState.ScanType.DL_FRONT
    override var backScanType: IdentityScanState.ScanType? = IdentityScanState.ScanType.DL_BACK
    override val fragmentId = R.id.driverLicenseUploadFragment
    override val presentedId = "DRIVER_LICENSE_UPLOAD_PRESENTED"
}
