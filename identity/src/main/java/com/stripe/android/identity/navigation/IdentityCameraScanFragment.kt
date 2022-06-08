package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import androidx.annotation.IdRes
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
import com.stripe.android.core.exception.InvalidResponseException
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.CouldNotCaptureFragment.Companion.ARG_COULD_NOT_CAPTURE_SCAN_TYPE
import com.stripe.android.identity.navigation.CouldNotCaptureFragment.Companion.ARG_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
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

    @get:IdRes
    protected abstract val fragmentId: Int

    @VisibleForTesting
    internal lateinit var cameraAdapter: Camera1Adapter

    /**
     * [CameraView] to initialize [Camera1Adapter], subclasses needs to set its value in
     * [Fragment.onCreateView].
     */
    protected lateinit var cameraView: CameraView

    /**
     * Called back at end of [onViewCreated] when permission is granted.
     */
    protected abstract fun onCameraReady()

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
                            verificationPage,
                            identityScanViewModel.targetScanType
                        )
                    } else if (finalResult.identityState is IdentityScanState.TimeOut) {
                        findNavController().navigate(
                            R.id.action_global_couldNotCaptureFragment,
                            bundleOf(
                                ARG_COULD_NOT_CAPTURE_SCAN_TYPE to identityScanViewModel.targetScanType
                            ).also {
                                if (identityScanViewModel.targetScanType != IdentityScanState.ScanType.SELFIE) {
                                    it.putBoolean(
                                        ARG_REQUIRE_LIVE_CAPTURE,
                                        verificationPage.documentCapture.requireLiveCapture
                                    )
                                }
                            }
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
        cameraAdapter = createCameraAdapter()

        identityViewModel.pageAndModelFiles.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    requireNotNull(it.data).let { pageAndModelFiles ->
                        identityScanViewModel.initializeScanFlow(
                            pageAndModelFiles.page,
                            idDetectorModelFile = pageAndModelFiles.idDetectorFile,
                            faceDetectorModelFile = pageAndModelFiles.faceDetectorFile
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

    protected open fun createCameraAdapter() =
        Camera1Adapter(
            requireNotNull(activity),
            cameraView.previewFrame,
            MINIMUM_RESOLUTION,
            DefaultCameraErrorListener(requireNotNull(activity)) { cause ->
                Log.e(TAG, "scan fails with exception: $cause")
            }
        )

    /**
     * Called back each time when [CameraViewModel.displayStateChanged] is changed.
     */
    protected abstract fun updateUI(identityScanState: IdentityScanState)

    protected abstract fun resetUI()

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
    protected fun stopScanning() {
        identityScanViewModel.identityScanFlow.resetFlow()
        cameraAdapter.unbindFromLifecycle(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Cancelling IdentityScanFlow")
        identityScanViewModel.identityScanFlow.cancelFlow()
    }

    internal companion object {
        private val TAG: String = IdentityCameraScanFragment::class.java.simpleName
        val MINIMUM_RESOLUTION = Size(1067, 600)
    }
}
