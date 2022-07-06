package com.stripe.android.identity.navigation

import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.IdentityIO

/**
 * Fragment to upload ID.
 */
internal class IDUploadFragment(
    identityIO: IdentityIO,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityUploadFragment(identityIO, identityViewModelFactory) {
    override val titleRes = R.string.file_upload
    override val contextRes = R.string.file_upload_content_id
    override val frontTextRes = R.string.front_of_id
    override var backTextRes: Int? = R.string.back_of_id
    override val frontCheckMarkContentDescription = R.string.front_of_id_selected
    override var backCheckMarkContentDescription: Int? = R.string.back_of_id_selected
    override val frontScanType = IdentityScanState.ScanType.ID_FRONT
    override var backScanType: IdentityScanState.ScanType? = IdentityScanState.ScanType.ID_BACK
    override val fragmentId = R.id.IDUploadFragment
    override val presentedId = "ID_UPLOAD_PRESENTED"
}
