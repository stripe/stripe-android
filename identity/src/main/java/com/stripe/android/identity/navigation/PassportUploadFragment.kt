package com.stripe.android.identity.navigation

import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState

/**
 * Fragment to upload passport.
 */
internal open class PassportUploadFragment(
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityUploadFragment(identityViewModelFactory) {
    override val titleRes = R.string.file_upload
    override val contextRes = R.string.file_upload_content_passport
    override val frontTextRes = R.string.passport
    override val frontCheckMarkContentDescription = R.string.passport_selected
    override val frontScanType = IdentityScanState.ScanType.PASSPORT
    override val collectedDataParamType = CollectedDataParam.Type.PASSPORT
    override val destinationRoute = PassportUploadDestination.ROUTE
}
