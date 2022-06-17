package com.stripe.android.identity.navigation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.cardview.widget.CardView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.camera.Camera1Adapter
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.camera.framework.image.mirrorHorizontally
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.databinding.SelfieScanFragmentBinding
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.LoadingButton
import com.stripe.android.identity.ui.SelfieResultAdapter
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.navigateToErrorFragmentWithFailedReason
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.utils.setHtmlString
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment to capture selfie.
 */
internal class SelfieFragment(
    identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityCameraScanFragment(identityCameraScanViewModelFactory, identityViewModelFactory) {
    override val fragmentId = R.id.selfieFragment

    private lateinit var flashAnimatorSet: AnimatorSet
    private lateinit var binding: SelfieScanFragmentBinding
    private lateinit var continueButton: LoadingButton
    private lateinit var messageView: TextView
    private lateinit var flashMask: View
    private lateinit var scanningView: CardView
    private lateinit var resultView: LinearLayout
    private lateinit var capturedImages: RecyclerView
    private lateinit var allowImageCollection: CheckBox

    internal var flashed = false

    @VisibleForTesting
    internal val selfieResultAdapter: SelfieResultAdapter = SelfieResultAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SelfieScanFragmentBinding.inflate(inflater, container, false)
        cameraView = binding.cameraView
        continueButton = binding.kontinue
        messageView = binding.message
        flashMask = binding.flashMask
        scanningView = binding.scanningView
        resultView = binding.resultView
        capturedImages = binding.capturedImages
        allowImageCollection = binding.allowImageCollection
        capturedImages.adapter = selfieResultAdapter

        flashAnimatorSet = AnimatorSet().apply {
            playSequentially(
                ObjectAnimator.ofFloat(flashMask, "alpha", FLASH_MAX_ALPHA).setDuration(
                    FLASH_ANIMATION_TIME
                ),
                ObjectAnimator.ofFloat(flashMask, "alpha", 0f).setDuration(
                    FLASH_ANIMATION_TIME
                )
            )
        }

        messageView.setText(R.string.position_selfie)
        continueButton.setText(getString(R.string.kontinue))
        continueButton.isEnabled = false

        continueButton.setOnClickListener {
            continueButton.toggleToLoading()
            collectUploadedStateAndUploadForCollectedSelfies()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        identityViewModel.resetSelfieUploadedState()
        identityViewModel.observeForVerificationPage(
            viewLifecycleOwner,
            onSuccess = {
                binding.allowImageCollection.setHtmlString(
                    requireNotNull(it.selfieCapture) { "VerificationPage.selfieCapture is null" }.consentText
                )
            },
            onFailure = {
                Log.e(TAG, "Failed to get verificationPage: $it")
                navigateToErrorFragmentWithFailedReason(
                    it ?: IllegalStateException("Failed to get verificationPage")
                )
            }
        )

        identityViewModel.sendAnalyticsRequest(
            identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                screenName = IdentityAnalyticsRequestFactory.SCREEN_NAME_SELFIE
            )
        )
    }

    override fun createCameraAdapter(): Camera1Adapter {
        return Camera1Adapter(
            requireNotNull(activity),
            cameraView.previewFrame,
            MINIMUM_RESOLUTION,
            DefaultCameraErrorListener(requireNotNull(activity)) { cause ->
                Log.e(TAG, "scan fails with exception: $cause")
                identityViewModel.sendAnalyticsRequest(
                    identityViewModel.identityAnalyticsRequestFactory.cameraError(
                        scanType = IdentityScanState.ScanType.SELFIE,
                        throwable = IllegalStateException(cause)
                    )
                )
            },
            false
        )
    }

    override fun onCameraReady() {
        startScanning(IdentityScanState.ScanType.SELFIE)
    }

    override fun updateUI(identityScanState: IdentityScanState) {
        when (identityScanState) {
            is IdentityScanState.Initial -> {
                // no-op
            }
            is IdentityScanState.Found -> {
                maybeFlash()
                binding.message.text = requireContext().getText(R.string.capturing)
            }
            is IdentityScanState.Unsatisfied -> {} // no-op
            is IdentityScanState.Satisfied -> {
                binding.message.text = requireContext().getText(R.string.selfie_capture_complete)
            }
            is IdentityScanState.Finished -> {
                toggleResultViewWithResult(
                    (identityScanState.transitioner as FaceDetectorTransitioner)
                        .filteredFrames.map { it.first.cameraPreviewImage.image.mirrorHorizontally() }
                )
            }
            is IdentityScanState.TimeOut -> {
                // no-op, transitions to CouldNotCaptureFragment
            }
        }
    }

    override fun resetUI() {
        scanningView.visibility = View.VISIBLE
        resultView.visibility = View.GONE
        continueButton.isEnabled = false
        messageView.text = requireContext().getText(R.string.position_selfie)
    }

    /**
     * Collect the [IdentityViewModel.selfieUploadState] and update UI accordingly.
     *
     * Try to [postVerificationPageDataAndMaybeSubmit] when all images are uploaded and navigates
     * to error when error occurs.
     */
    private fun collectUploadedStateAndUploadForCollectedSelfies() =
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                identityViewModel.selfieUploadState.collectLatest {
                    when {
                        it.hasError() -> {
                            Log.e(TAG, "Fail to upload files: ${it.getError()}")
                            navigateToDefaultErrorFragment()
                        }
                        it.isAnyLoading() -> {
                            continueButton.toggleToLoading()
                        }
                        it.isAllUploaded() -> {
                            runCatching {
                                val faceDetectorTransitioner =
                                    requireNotNull(
                                        identityScanViewModel.finalResult.value?.identityState?.transitioner as? FaceDetectorTransitioner
                                    ) {
                                        "Failed to retrieve final result for Selfie"
                                    }
                                postVerificationPageDataAndMaybeSubmit(
                                    identityViewModel = identityViewModel,
                                    collectedDataParam = CollectedDataParam.createForSelfie(
                                        firstHighResResult = requireNotNull(it.firstHighResResult.data),
                                        firstLowResResult = requireNotNull(it.firstLowResResult.data),
                                        lastHighResResult = requireNotNull(it.lastHighResResult.data),
                                        lastLowResResult = requireNotNull(it.lastLowResResult.data),
                                        bestHighResResult = requireNotNull(it.bestHighResResult.data),
                                        bestLowResResult = requireNotNull(it.bestLowResResult.data),
                                        trainingConsent = allowImageCollection.isChecked,
                                        faceScoreVariance = faceDetectorTransitioner.scoreVariance,
                                        bestFaceScore = faceDetectorTransitioner.bestFaceScore,
                                        numFrames = faceDetectorTransitioner.numFrames
                                    ),
                                    fromFragment = fragmentId,
                                    clearDataParam = ClearDataParam.SELFIE_TO_CONFIRM,
                                )
                            }.onFailure { throwable ->
                                Log.e(
                                    TAG,
                                    "fail to submit uploaded files: $throwable"
                                )
                                navigateToDefaultErrorFragment()
                            }
                        }
                        else -> {
                            Log.e(
                                TAG,
                                "collectUploadedStateAndUploadForCollectedSelfies reaches unexpected upload state: $it"
                            )
                            navigateToDefaultErrorFragment()
                        }
                    }
                }
            }
        }

    /**
     * Animate the selfie view with a flash.
     */
    private fun maybeFlash() {
        if (!flashed) {
            flashAnimatorSet.start()
            flashed = true
        }
    }

    private fun toggleResultViewWithResult(resultList: List<Bitmap>) {
        scanningView.visibility = View.GONE
        resultView.visibility = View.VISIBLE
        continueButton.isEnabled = true
        selfieResultAdapter.submitList(resultList)
    }

    internal companion object {
        private val TAG: String = SelfieFragment::class.java.simpleName
        private const val FLASH_MAX_ALPHA = 0.5f
        private const val FLASH_ANIMATION_TIME = 200L
    }
}
