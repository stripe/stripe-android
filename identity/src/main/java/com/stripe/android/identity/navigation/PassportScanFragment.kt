package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    override val fragmentId = R.id.passportScanFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        headerTitle.text = requireContext().getText(R.string.passport)
        messageView.text = requireContext().getText(R.string.position_passport)
        continueButton.setOnClickListener {
            continueButton.toggleToLoading()
            collectUploadedStateAndUploadForFrontSide()
        }
    }

    /**
     * Collect the [IdentityViewModel.documentUploadState] and upload when frontHighRes and frontLowRes
     * are uploaded.
     *
     * Try to [postVerificationPageDataAndMaybeSubmit] when success and navigates to error when fails.
     */
    private fun collectUploadedStateAndUploadForFrontSide() =
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                identityViewModel.documentUploadState.collectLatest {
                    when {
                        it.hasError() -> {
                            navigateToDefaultErrorFragment()
                        }
                        it.isFrontLoading() -> {
                            continueButton.toggleToLoading()
                        }
                        it.isFrontUploaded() -> {
                            lifecycleScope.launch {
                                runCatching {
                                    postVerificationPageDataAndMaybeSubmit(
                                        identityViewModel = identityViewModel,
                                        collectedDataParam =
                                        CollectedDataParam.createFromUploadedResultsForAutoCapture(
                                            type = CollectedDataParam.Type.PASSPORT,
                                            frontHighResResult = requireNotNull(it.frontHighResResult.data),
                                            frontLowResResult = requireNotNull(it.frontLowResResult.data)
                                        ),
                                        clearDataParam = ClearDataParam.UPLOAD_TO_CONFIRM,
                                        fromFragment = R.id.passportScanFragment,
                                        shouldNotSubmit = { false }
                                    )
                                }.onFailure { throwable ->
                                    Log.e(
                                        TAG,
                                        "fail to submit uploaded files: $throwable"
                                    )
                                    navigateToDefaultErrorFragment()
                                }
                            }
                        }
                        else -> {
                            Log.d(
                                TAG,
                                "observeAndUploadForFrontSide reaches unexpected upload state: $it"
                            )
                            navigateToDefaultErrorFragment()
                        }
                    }
                }
            }
        }

    override fun onCameraReady() {
        startScanning(IdentityScanState.ScanType.PASSPORT)
    }

    override fun resetUI() {
        super.resetUI()
        headerTitle.text = requireContext().getText(R.string.passport)
        messageView.text = requireContext().getText(R.string.position_passport)
    }

    internal companion object {
        val TAG: String = PassportScanFragment::class.java.simpleName
    }
}
