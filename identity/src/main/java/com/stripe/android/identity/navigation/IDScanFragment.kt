package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.Camera1Adapter
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.IdScanFragmentBinding
import com.stripe.android.identity.states.ScanState
import com.stripe.android.identity.viewmodel.CameraViewModel

/**
 * TODO(ccen) connect the logic to initialize CameraAdapter and call IdentityActivity#ensureCameraPermission
 */
internal class IDScanFragment : Fragment() {
    private val cameraViewModel: CameraViewModel by viewModels()
    private lateinit var binding: IdScanFragmentBinding
    private lateinit var cameraAdapter: Camera1Adapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = IdScanFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraViewModel.interimResults.observe(viewLifecycleOwner) {
            Log.d(TAG, "IDScanFragment get interim result: $it")
        }

        cameraViewModel.finalResult.observe(viewLifecycleOwner) {
            Log.d(TAG, "IDScanFragment get final result: $it")
        }

        cameraViewModel.reset.observe(viewLifecycleOwner) {
            Log.d(TAG, "IDScanFragment get reset")
        }

        cameraAdapter = Camera1Adapter(
            requireNotNull(activity),
            binding.cameraView.previewFrame,
            MINIMUM_RESOLUTION,
            DefaultCameraErrorListener(requireNotNull(activity)) { cause ->
                Log.d(TAG, "scan fails with exception: $cause")
                // TODO(ccen) determine if further handling is required
            }
        )
        cameraViewModel.initializeScanFlow(
            ScanState.ScanType.ID_FRONT // determine how to transition to ID_BACK
        )
        ensureCameraPermissionFromIdentityActivity(
            onCameraReady = {
                Log.d(TAG, "Camera permission granted")
                cameraAdapter.bindToLifecycle(this)
                cameraViewModel.identityScanFlow.startFlow(
                    context = requireContext(),
                    imageStream = cameraAdapter.getImageStream(),
                    viewFinder = binding.cameraView.viewFinderWindowView.asRect(),
                    lifecycleOwner = this,
                    coroutineScope = lifecycleScope,
                    parameters = 23 // TODO(ccen) pass correct parameter
                )
            },

            onUserDeniedCameraPermission = {
                Log.d(TAG, "Camera permission denied")
                findNavController().navigate(
                    R.id.action_camera_permission_denied,
                    bundleOf(
                        CameraPermissionDeniedFragment.ARG_SCAN_TYPE to ScanState.ScanType.ID_FRONT
                    )
                )
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Cancelling IdentityScanFlow")
        cameraViewModel.identityScanFlow.cancelFlow()
    }

    private companion object {
        val TAG: String = IDScanFragment::class.java.simpleName
        val MINIMUM_RESOLUTION = Size(1067, 600) // TODO: decide what to use
    }
}
