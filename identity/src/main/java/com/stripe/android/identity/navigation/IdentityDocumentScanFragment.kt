package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.Camera1Adapter
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.camera.scanui.util.startAnimation
import com.stripe.android.camera.scanui.util.startAnimationIfNotRunning
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.databinding.IdentityDocumentScanFragmentBinding
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.LoadingButton
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.viewmodel.CameraViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment for scanning ID, Passport and Driver's license
 */
internal abstract class IdentityDocumentScanFragment(
    identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityCameraScanFragment(
    identityCameraScanViewModelFactory, identityViewModelFactory
) {
    abstract val frontScanType: IdentityScanState.ScanType

    protected lateinit var binding: IdentityDocumentScanFragmentBinding
    protected lateinit var headerTitle: TextView
    protected lateinit var messageView: TextView
    protected lateinit var continueButton: LoadingButton
    private lateinit var checkMarkView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = IdentityDocumentScanFragmentBinding.inflate(inflater, container, false)
        cameraView = binding.cameraView

        cameraView.viewFinderWindowView.setBackgroundResource(R.drawable.viewfinder_background)

        headerTitle = binding.headerTitle
        messageView = binding.message

        checkMarkView = binding.checkMarkView
        continueButton = binding.kontinue
        continueButton.setText(getString(R.string.kontinue))
        continueButton.isEnabled = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!shouldStartFromBack()) {
            identityViewModel.resetDocumentUploadedState()
        }
        super.onViewCreated(view, savedInstanceState)

        identityViewModel.sendAnalyticsRequest(
            identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                scanType = frontScanType,
                screenName = IdentityAnalyticsRequestFactory.SCREEN_NAME_LIVE_CAPTURE
            )
        )
    }

    /**
     * Check if should start scanning from back.
     */
    protected fun shouldStartFromBack(): Boolean =
        arguments?.get(ARG_SHOULD_START_FROM_BACK) as? Boolean == true

    override fun resetUI() {
        cameraView.viewFinderBackgroundView.visibility = View.VISIBLE
        cameraView.viewFinderWindowView.visibility = View.VISIBLE
        cameraView.viewFinderBorderView.visibility = View.VISIBLE
        continueButton.isEnabled = false
        checkMarkView.visibility = View.GONE
        cameraView.viewFinderBorderView.startAnimation(R.drawable.viewfinder_border_initial)
    }

    /**
     * Called back each time when [CameraViewModel.displayStateChanged] is changed.
     */
    override fun updateUI(identityScanState: IdentityScanState) {
        when (identityScanState) {
            is IdentityScanState.Initial -> {
                resetUI()
            }
            is IdentityScanState.Found -> {
                messageView.text = requireContext().getText(R.string.hold_still)
                cameraView.viewFinderBorderView.startAnimationIfNotRunning(R.drawable.viewfinder_border_found)
            }
            is IdentityScanState.Unsatisfied -> {} // no-op
            is IdentityScanState.Satisfied -> {
                messageView.text = requireContext().getText(R.string.scanned)
            }
            is IdentityScanState.Finished -> {
                cameraView.viewFinderBackgroundView.visibility = View.INVISIBLE
                cameraView.viewFinderWindowView.visibility = View.INVISIBLE
                cameraView.viewFinderBorderView.visibility = View.INVISIBLE
                checkMarkView.visibility = View.VISIBLE
                continueButton.isEnabled = true
                messageView.text = requireContext().getText(R.string.scanned)
                cameraView.viewFinderBorderView.startAnimation(R.drawable.viewfinder_border_initial)
            }
            is IdentityScanState.TimeOut -> {
                // no-op, transitions to CouldNotCaptureFragment
            }
        }
    }

    override fun createCameraAdapter() = Camera1Adapter(
        requireNotNull(activity),
        cameraView.previewFrame,
        MINIMUM_RESOLUTION,
        DefaultCameraErrorListener(requireNotNull(activity)) { cause ->
            Log.e(TAG, "scan fails with exception: $cause")
            identityViewModel.sendAnalyticsRequest(
                identityViewModel.identityAnalyticsRequestFactory.cameraError(
                    scanType = frontScanType,
                    throwable = IllegalStateException(cause)
                )
            )
        }
    )

    /**
     * Collect the [IdentityViewModel.documentUploadState] and update UI accordingly.
     *
     * Try to [postVerificationPageDataAndMaybeSubmit] when all images are uploaded and navigates
     * to error when error occurs.
     */
    protected fun collectUploadedStateAndUploadForBothSides(type: CollectedDataParam.Type) =
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                identityViewModel.documentUploadState.collectLatest {
                    when {
                        it.hasError() -> {
                            Log.e(TAG, "Fail to upload files: ${it.getError()}")
                            navigateToDefaultErrorFragment()
                        }
                        it.isAnyLoading() -> {
                            continueButton.toggleToLoading()
                        }
                        it.isBothUploaded() -> {
                            identityViewModel.observeForVerificationPage(
                                viewLifecycleOwner,
                                onSuccess = { verificationPage ->
                                    lifecycleScope.launch {
                                        runCatching {
                                            if (verificationPage.requireSelfie()) {
                                                postVerificationPageDataAndMaybeSubmit(
                                                    identityViewModel = identityViewModel,
                                                    collectedDataParam =
                                                    CollectedDataParam.createFromUploadedResultsForAutoCapture(
                                                        type = type,
                                                        frontHighResResult = requireNotNull(it.frontHighResResult.data),
                                                        frontLowResResult = requireNotNull(it.frontLowResResult.data),
                                                        backHighResResult = requireNotNull(it.backHighResResult.data),
                                                        backLowResResult = requireNotNull(it.backLowResResult.data)
                                                    ),
                                                    clearDataParam = ClearDataParam.UPLOAD_TO_SELFIE,
                                                    fromFragment = fragmentId
                                                ) {
                                                    findNavController().navigate(R.id.action_global_selfieFragment)
                                                }
                                            } else {
                                                postVerificationPageDataAndMaybeSubmit(
                                                    identityViewModel = identityViewModel,
                                                    collectedDataParam =
                                                    CollectedDataParam.createFromUploadedResultsForAutoCapture(
                                                        type = type,
                                                        frontHighResResult = requireNotNull(it.frontHighResResult.data),
                                                        frontLowResResult = requireNotNull(it.frontLowResResult.data),
                                                        backHighResResult = requireNotNull(it.backHighResResult.data),
                                                        backLowResResult = requireNotNull(it.backLowResResult.data)
                                                    ),
                                                    clearDataParam = ClearDataParam.UPLOAD_TO_CONFIRM,
                                                    fromFragment = fragmentId
                                                )
                                            }
                                        }.onFailure { throwable ->
                                            Log.e(
                                                TAG,
                                                "fail to submit uploaded files: $throwable"
                                            )
                                            navigateToDefaultErrorFragment()
                                        }
                                    }
                                },
                                onFailure = { throwable ->
                                    Log.e(TAG, "Fail to observeForVerificationPage: $throwable")
                                    navigateToDefaultErrorFragment()
                                }
                            )
                        }
                        else -> {
                            Log.e(
                                TAG,
                                "observeAndUploadForBothSides reaches unexpected upload state: $it"
                            )
                            navigateToDefaultErrorFragment()
                        }
                    }
                }
            }
        }

    internal companion object {
        const val ARG_SHOULD_START_FROM_BACK = "startFromBack"
        private val TAG: String = IdentityDocumentScanFragment::class.java.simpleName
    }
}
