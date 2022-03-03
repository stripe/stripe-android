package com.stripe.android.identity.navigation

import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.states.IdentityScanState

/**
 * Fragment to upload ID.
 */
internal class IDUploadFragment(
    frontBackUploadViewModelFactory: ViewModelProvider.Factory
) : FrontBackUploadFragment(frontBackUploadViewModelFactory) {
    override val titleRes = R.string.file_upload
    override val contextRes = R.string.file_upload_content_id
    override val frontTextRes = R.string.front_of_id
    override val backTextRes = R.string.back_of_id
    override val frontCheckMarkContentDescription = R.string.front_of_id_selected
    override val backCheckMarkContentDescription = R.string.back_of_id_selected
    override val continueButtonNavigationId = R.id.action_IDUploadFragment_to_confirmationFragment
    override val frontScanType = IdentityScanState.ScanType.ID_FRONT
    override val backScanType = IdentityScanState.ScanType.ID_BACK
}
