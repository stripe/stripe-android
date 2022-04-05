package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.Camera1Adapter
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.camera.scanui.util.startAnimation
import com.stripe.android.camera.scanui.util.startAnimationIfNotRunning
import com.stripe.android.core.exception.InvalidResponseException
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.IdentityCameraScanFragmentBinding
import com.stripe.android.identity.navigation.CouldNotCaptureFragment.Companion.ARG_COULD_NOT_CAPTURE_SCAN_TYPE
import com.stripe.android.identity.navigation.CouldNotCaptureFragment.Companion.ARG_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.LoadingButton
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.viewmodel.CameraViewModel
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * An abstract [Fragment] class to access camera scanning for Identity.
 *
 * Subclasses are responsible for populating [cameraView] in its [Fragment.onCreateView] method.
 */
internal abstract class IdentityCameraScanFragment(
    private val identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    private val identityViewModelFactory: ViewModelProvider.Factory
) : Fragment() {
    protected val identityScanViewModel: IdentityScanViewModel by viewModels { identityCameraScanViewModelFactory }
    protected val identityViewModel: IdentityViewModel by activityViewModels { identityViewModelFactory }

    @VisibleForTesting
    internal lateinit var cameraAdapter: Camera1Adapter

    /**
     * [CameraView] to initialize [Camera1Adapter], subclasses needs to set its value in
     * [Fragment.onCreateView].
     */
    protected lateinit var cameraView: CameraView
    protected lateinit var binding: IdentityCameraScanFragmentBinding
    protected lateinit var headerTitle: TextView
    protected lateinit var messageView: TextView
    protected lateinit var continueButton: LoadingButton
    private lateinit var checkMarkView: ImageView

    /**
     * Called back at end of [onViewCreated] when permission is granted.
     */
    protected abstract fun onCameraReady()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = IdentityCameraScanFragmentBinding.inflate(inflater, container, false)
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
        super.onViewCreated(view, savedInstanceState)
        identityScanViewModel.displayStateChanged.observe(viewLifecycleOwner) { (newState, _) ->
            updateUI(newState)
        }
        identityScanViewModel.finalResult.observe(viewLifecycleOwner) { finalResult ->
            identityViewModel.observeForVerificationPage(
                viewLifecycleOwner,
                onSuccess = { verificationPage ->
                    if (finalResult.identityState is IdentityScanState.Finished) {
                        identityViewModel.uploadScanResult(
                            finalResult,
                            verificationPage.documentCapture,
                            identityScanViewModel.targetScanType
                        )
                    } else if (finalResult.identityState is IdentityScanState.TimeOut) {
                        findNavController().navigate(
                            R.id.action_global_couldNotCaptureFragment,
                            bundleOf(
                                ARG_COULD_NOT_CAPTURE_SCAN_TYPE to identityScanViewModel.targetScanType,
                                ARG_REQUIRE_LIVE_CAPTURE to verificationPage.documentCapture.requireLiveCapture
                            )
                        )
                    }
                },
                onFailure = {
                    Log.e(TAG, "Fail to observeForVerificationPage: $it")
                    navigateToDefaultErrorFragment()
                }
            )
            stopScanning()
        }
        cameraAdapter = Camera1Adapter(
            requireNotNull(activity),
            cameraView.previewFrame,
            MINIMUM_RESOLUTION,
            DefaultCameraErrorListener(requireNotNull(activity)) { cause ->
                Log.e(TAG, "scan fails with exception: $cause")
            }
        )

        identityViewModel.pageAndModel.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    requireNotNull(it.data).let { pageFilePair ->
                        identityScanViewModel.initializeScanFlow(
                            pageFilePair.first,
                            pageFilePair.second
                        )
                        lifecycleScope.launch(Dispatchers.Main) {
                            onCameraReady()
                        }
                    }
                }
                Status.LOADING -> {} // no-op
                Status.ERROR -> {
                    throw InvalidResponseException(
                        cause = it.throwable,
                        message = it.message
                    )
                }
            }
        }
    }

    /**
     * Check if should start scanning from back.
     */
    protected fun shouldStartFromBack(): Boolean =
        arguments?.get(ARG_SHOULD_START_FROM_BACK) as? Boolean == true

    /**
     * Called back each time when [CameraViewModel.displayStateChanged] is changed.
     */
    protected open fun updateUI(identityScanState: IdentityScanState) {
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

    protected open fun resetUI() {
        cameraView.viewFinderBackgroundView.visibility = View.VISIBLE
        cameraView.viewFinderWindowView.visibility = View.VISIBLE
        cameraView.viewFinderBorderView.visibility = View.VISIBLE
        continueButton.isEnabled = false
        checkMarkView.visibility = View.GONE
        cameraView.viewFinderBorderView.startAnimation(R.drawable.viewfinder_border_initial)
    }

    /**
     * Start scanning for the required scan type.
     */
    protected fun startScanning(scanType: IdentityScanState.ScanType) {
        identityScanViewModel.targetScanType = scanType
        resetUI()
        cameraAdapter.bindToLifecycle(this)
        identityScanViewModel.scanState = null
        identityScanViewModel.scanStatePrevious = null
        identityScanViewModel.identityScanFlow.startFlow(
            context = requireContext(),
            imageStream = cameraAdapter.getImageStream(),
            viewFinder = cameraView.viewFinderWindowView.asRect(),
            lifecycleOwner = viewLifecycleOwner,
            coroutineScope = lifecycleScope,
            parameters = scanType
        )
    }

    /**
     * Stop scanning, may start again later.
     */
    private fun stopScanning() {
        identityScanViewModel.identityScanFlow.resetFlow()
        cameraAdapter.unbindFromLifecycle(this)
    }

    /**
     * Observe for [IdentityViewModel.bothUploaded],
     * try to [postVerificationPageDataAndMaybeSubmit] when success and navigates to error when fails.
     */
    protected fun observeAndUploadForBothSides(type: CollectedDataParam.Type) =
        identityViewModel.bothUploaded.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    it.data?.let { uploadedFiles ->
                        lifecycleScope.launch {
                            runCatching {
                                postVerificationPageDataAndMaybeSubmit(
                                    identityViewModel = identityViewModel,
                                    collectedDataParam =
                                    CollectedDataParam.createFromUploadedResultsForAutoCapture(
                                        type = type,
                                        frontHighResResult = uploadedFiles.first.first,
                                        frontLowResResult = uploadedFiles.first.second,
                                        backHighResResult = uploadedFiles.second.first,
                                        backLowResResult = uploadedFiles.second.second,
                                    ),
                                    clearDataParam = ClearDataParam.UPLOAD_TO_CONFIRM,
                                    shouldNotSubmit = { false }
                                )
                            }.onFailure { throwable ->
                                Log.d(
                                    TAG,
                                    "fail to submit uploaded files: $throwable"
                                )
                                navigateToDefaultErrorFragment()
                            }
                        }
                    }
                }
                Status.ERROR -> {
                    Log.e(TAG, "Fail to upload files: ${it.throwable}")
                    navigateToDefaultErrorFragment()
                }
                Status.LOADING -> {
                    continueButton.toggleToLoading()
                }
            }
        }

    /**
     * Observe for [IdentityViewModel.frontUploaded],
     * try to [postVerificationPageDataAndMaybeSubmit] when success and navigates to error when fails.
     */
    protected fun observeAndUploadForFrontSide(type: CollectedDataParam.Type) =
        identityViewModel.frontUploaded.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    it.data?.let { uploadedFiles ->
                        val frontHighResResult = uploadedFiles.first
                        val frontLowResResult = uploadedFiles.second
                        lifecycleScope.launch {
                            runCatching {
                                postVerificationPageDataAndMaybeSubmit(
                                    identityViewModel = identityViewModel,
                                    collectedDataParam =
                                    CollectedDataParam.createFromUploadedResultsForAutoCapture(
                                        type,
                                        frontHighResResult,
                                        frontLowResResult
                                    ),
                                    clearDataParam = ClearDataParam.UPLOAD_TO_CONFIRM,
                                    shouldNotSubmit = { false }
                                )
                            }.onFailure { throwable ->
                                Log.d(
                                    PassportScanFragment.TAG,
                                    "fail to submit uploaded files: $throwable"
                                )
                                navigateToDefaultErrorFragment()
                            }
                        }
                    }
                }
                Status.ERROR -> {
                    Log.e(PassportScanFragment.TAG, "Fail to upload files: ${it.throwable}")
                    navigateToDefaultErrorFragment()
                }
                Status.LOADING -> {
                    continueButton.toggleToLoading()
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Cancelling IdentityScanFlow")
        identityScanViewModel.identityScanFlow.cancelFlow()
    }

    internal companion object {
        const val ARG_SHOULD_START_FROM_BACK = "startFromBack"
        private val TAG: String = IdentityCameraScanFragment::class.java.simpleName
        private val MINIMUM_RESOLUTION = Size(1067, 600) // TODO: decide what to use
    }
}
