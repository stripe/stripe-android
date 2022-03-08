package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.camera.Camera1Adapter
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.core.exception.InvalidResponseException
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.CameraViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * An abstract [Fragment] class to access camera scanning for Identity.
 *
 * Subclasses are responsible for populating [cameraView] in its [Fragment.onCreateView] method.
 *
 * When the fragment's view is created, [cameraPermissionEnsureable] is used to check camera
 * permission. Subclasses are responsible for implementing [onCameraReady] and
 * [onUserDeniedCameraPermission] to handle the permission callbacks.
 *
 */
internal abstract class IdentityCameraScanFragment(
    private val cameraPermissionEnsureable: CameraPermissionEnsureable,
    private val cameraViewModelFactory: ViewModelProvider.Factory,
    private val identityViewModelFactory: ViewModelProvider.Factory
) : Fragment() {
    protected val cameraViewModel: CameraViewModel by viewModels { cameraViewModelFactory }
    private val identityViewModel: IdentityViewModel by activityViewModels { identityViewModelFactory }

    @VisibleForTesting
    internal lateinit var cameraAdapter: Camera1Adapter

    /**
     * [CameraView] to initialize [Camera1Adapter], subclasses needs to set its value in
     * [Fragment.onCreateView].
     */
    protected lateinit var cameraView: CameraView

    /**
     * Called back once after at end of [onViewCreated] when permission is granted.
     */
    protected abstract fun onCameraReady()

    /**
     * Called back once after at end of [onViewCreated] when permission is denied.
     */
    protected abstract fun onUserDeniedCameraPermission()

    /**
     * Called back each time when [CameraViewModel.displayStateChanged] is changed.
     */
    protected abstract fun updateUI(identityScanState: IdentityScanState)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraViewModel.displayStateChanged.observe(viewLifecycleOwner) { (newState, _) ->
            updateUI(newState)
        }
        cameraViewModel.finalResult.observe(viewLifecycleOwner) {
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

        identityViewModel.idDetectorModelFile.observe(viewLifecycleOwner) {
            cameraViewModel.initializeScanFlow(it)
            cameraPermissionEnsureable.ensureCameraPermission(
                ::onCameraReady,
                ::onUserDeniedCameraPermission
            )
        }

        identityViewModel.idDetectorModelError.observe(viewLifecycleOwner) {
            throw InvalidResponseException(
                cause = it,
                message = "Fail to download IDDetector model."
            )
        }
    }

    /**
     * Start scanning for the required scan type.
     */
    protected fun startScanning(scanType: IdentityScanState.ScanType) {
        cameraAdapter.bindToLifecycle(this)
        // TODO(ccen): pack this logic into a IdentitySpecific CameraViewModel
        cameraViewModel.scanState = null
        cameraViewModel.scanStatePrevious = null
        cameraViewModel.identityScanFlow.startFlow(
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
        cameraViewModel.identityScanFlow.resetFlow()
        cameraAdapter.unbindFromLifecycle(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Cancelling IdentityScanFlow")
        cameraViewModel.identityScanFlow.cancelFlow()
    }

    private companion object {
        val TAG: String = IdentityCameraScanFragment::class.java.simpleName
        val MINIMUM_RESOLUTION = Size(1067, 600) // TODO: decide what to use
    }
}
