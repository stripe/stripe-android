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
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.Camera1Adapter
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.camera.scanui.util.startAnimation
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.IdScanFragmentBinding
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.CameraViewModel

/**
 * Fragment to scan the ID.
 */
internal class IDScanFragment(
    private val cameraPermissionEnsureable: CameraPermissionEnsureable,
    private val cameraViewModelFactory: ViewModelProvider.Factory
) : Fragment() {
    private val cameraViewModel: CameraViewModel by viewModels { cameraViewModelFactory }
    private lateinit var binding: IdScanFragmentBinding
    private lateinit var messageView: TextView
    private lateinit var cameraView: CameraView
    private lateinit var checkMarkView: ImageView

    @VisibleForTesting
    internal lateinit var cameraAdapter: Camera1Adapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = IdScanFragmentBinding.inflate(inflater, container, false)
        messageView = binding.message
        cameraView = binding.cameraView
        checkMarkView = binding.checkMarkView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraViewModel.finalResult.observe(viewLifecycleOwner) {
            cameraViewModel.identityScanFlow.cancelFlow()
            cameraAdapter.unbindFromLifecycle(this)
        }

        cameraViewModel.displayStateChanged.observe(viewLifecycleOwner) { (newState, _) ->
            updateUI(newState)
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
            IdentityScanState.ScanType.ID_FRONT // determine how to transition to ID_BACK
        )

        cameraPermissionEnsureable.ensureCameraPermission(
            onCameraReady = {
                Log.d(TAG, "Camera permission granted")
                cameraAdapter.bindToLifecycle(this)
                cameraViewModel.identityScanFlow.startFlow(
                    context = requireContext(),
                    imageStream = cameraAdapter.getImageStream(),
                    viewFinder = binding.cameraView.viewFinderWindowView.asRect(),
                    lifecycleOwner = viewLifecycleOwner,
                    coroutineScope = lifecycleScope,
                    parameters = 23 // TODO(ccen) pass correct parameter
                )
            },

            onUserDeniedCameraPermission = {
                Log.d(TAG, "Camera permission denied")
                findNavController().navigate(
                    R.id.action_camera_permission_denied,
                    bundleOf(
                        CameraPermissionDeniedFragment.ARG_SCAN_TYPE to IdentityScanState.ScanType.ID_FRONT
                    )
                )
            }
        )
    }

    private fun updateUI(identityState: IdentityScanState) {
        when (identityState) {
            is IdentityScanState.Initial -> {
                cameraView.viewFinderBackgroundView.visibility = View.VISIBLE
                cameraView.viewFinderWindowView.visibility = View.VISIBLE
                cameraView.viewFinderBorderView.visibility = View.VISIBLE
                checkMarkView.visibility = View.GONE

                messageView.text = requireContext().getText(R.string.position_id_front)
                cameraView.viewFinderWindowView.setBackgroundResource(R.drawable.id_viewfinder_background)
                cameraView.viewFinderBorderView.startAnimation(R.drawable.id_border_initial)
            }
            is IdentityScanState.Found -> {
                messageView.text = requireContext().getText(R.string.hold_still)
                cameraView.viewFinderWindowView.setBackgroundResource(R.drawable.id_viewfinder_background)
                cameraView.viewFinderBorderView.startAnimation(R.drawable.id_border_found)
            }
            is IdentityScanState.Unsatisfied -> {
                messageView.text = requireContext().getText(R.string.position_id_in_center)
                cameraView.viewFinderWindowView.setBackgroundResource(R.drawable.id_viewfinder_background)
                cameraView.viewFinderBorderView.startAnimation(R.drawable.id_border_unsatisfied)
            }
            is IdentityScanState.Satisfied -> {
                messageView.text = requireContext().getText(R.string.scanned)
                cameraView.viewFinderWindowView.setBackgroundResource(R.drawable.id_viewfinder_background)
                cameraView.viewFinderBorderView.startAnimation(R.drawable.id_border_satisfied)
            }
            is IdentityScanState.Finished -> {
                cameraView.viewFinderBackgroundView.visibility = View.INVISIBLE
                cameraView.viewFinderWindowView.visibility = View.INVISIBLE
                cameraView.viewFinderBorderView.visibility = View.INVISIBLE
                checkMarkView.visibility = View.VISIBLE

                messageView.text = requireContext().getText(R.string.scanned)
            }
        }
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
