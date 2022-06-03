package com.stripe.android.identity.navigation

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.DocumentUploadState
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import kotlinx.coroutines.launch

/**
 * Fragment to upload passport.
 */
internal open class PassportUploadFragment(
    identityUploadViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityUploadFragment(identityUploadViewModelFactory, identityViewModelFactory) {
    override val titleRes = R.string.file_upload
    override val contextRes = R.string.file_upload_content_passport
    override val frontTextRes = R.string.passport
    override val frontCheckMarkContentDescription = R.string.passport_selected
    override val frontScanType = IdentityScanState.ScanType.PASSPORT
    override val fragmentId = R.id.passportUploadFragment

    override fun showFrontDone(latestState: DocumentUploadState) {
        super.showFrontDone(latestState)
        binding.kontinue.isEnabled = true
        binding.kontinue.setOnClickListener {
            binding.kontinue.toggleToLoading()
            lifecycleScope.launch {
                runCatching {
                    val frontResult = requireNotNull(latestState.frontHighResResult.data)
                    trySubmit(
                        CollectedDataParam(
                            idDocumentFront = DocumentUploadParam(
                                highResImage = requireNotNull(frontResult.uploadedStripeFile.id) {
                                    "front uploaded file id is null"
                                },
                                uploadMethod = requireNotNull(frontResult.uploadMethod)
                            ),
                            idDocumentType = CollectedDataParam.Type.PASSPORT
                        )
                    )
                }.onFailure {
                    Log.d(TAG, "fail to submit uploaded files: $it")
                    navigateToDefaultErrorFragment()
                }
            }
        }
    }

    companion object {
        val TAG: String = PassportUploadFragment::class.java.simpleName
    }
}
