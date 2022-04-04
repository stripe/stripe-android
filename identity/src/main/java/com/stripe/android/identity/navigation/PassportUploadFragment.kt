package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * Fragment to upload passport.
 */
internal class PassportUploadFragment(
    identityUploadViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityUploadFragment(identityUploadViewModelFactory, identityViewModelFactory) {
    override val titleRes = R.string.file_upload
    override val contextRes = R.string.file_upload_content_passport
    override val frontTextRes = R.string.passport
    override val frontCheckMarkContentDescription = R.string.passport_selected
    override val frontScanType = IdentityScanState.ScanType.PASSPORT

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeForFrontUploaded()
    }

    override fun showFrontDone(frontResult: IdentityViewModel.UploadedResult?) {
        super.showFrontDone(frontResult)
        enableKontinueWhenFrontUploaded(frontResult)
    }

    private fun enableKontinueWhenFrontUploaded(frontResult: IdentityViewModel.UploadedResult?) {
        binding.kontinue.isEnabled = true
        binding.kontinue.setOnClickListener {
            binding.kontinue.toggleToLoading()
            lifecycleScope.launch {
                runCatching {
                    requireNotNull(frontResult)
                    postVerificationPageDataAndMaybeSubmit(
                        identityViewModel = identityViewModel,
                        collectedDataParam = CollectedDataParam(
                            idDocument = IdDocumentParam(
                                front = DocumentUploadParam(
                                    highResImage = requireNotNull(frontResult.uploadedStripeFile.id) {
                                        "front uploaded file id is null"
                                    },
                                    uploadMethod = frontResult.uploadMethod
                                ),
                                type = IdDocumentParam.Type.PASSPORT
                            )
                        ),
                        clearDataParam = ClearDataParam.UPLOAD_TO_CONFIRM,
                        shouldNotSubmit = { false }
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
