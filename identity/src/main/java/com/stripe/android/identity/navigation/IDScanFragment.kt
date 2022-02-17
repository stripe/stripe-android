package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import com.stripe.android.identity.states.IdentityScanState.ScanType.ID_BACK
import com.stripe.android.identity.states.IdentityScanState.ScanType.ID_FRONT
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
    private lateinit var headerTitle: TextView
    private lateinit var messageView: TextView
    private lateinit var cameraView: CameraView
    private lateinit var checkMarkView: ImageView
    private lateinit var continueButton: Button

    @VisibleForTesting
    internal lateinit var cameraAdapter: Camera1Adapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = IdScanFragmentBinding.inflate(inflater, container, false)
        headerTitle = binding.headerTitle
        messageView = binding.message
        cameraView = binding.cameraView
        checkMarkView = binding.checkMarkView
        continueButton = binding.kontinue
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        continueButton.setOnClickListener {
            when (cameraViewModel.targetScanType) {
                ID_FRONT -> {
                    startScanning(ID_BACK)
                    cameraViewModel.targetScanType = ID_BACK
                }
                ID_BACK -> {
                    findNavController().navigate(R.id.action_IDScanFragment_to_confirmationFragment)
                }
                else -> {
                    Log.e(TAG, "Incorrect target scan type: ${cameraViewModel.targetScanType}")
                }
            }
        }

        cameraViewModel.finalResult.observe(viewLifecycleOwner) {
            stopScanning()
        }

        cameraViewModel.displayStateChanged.observe(viewLifecycleOwner) { (newState, _) ->
            updateUI(newState)
        }

        cameraAdapter = Camera1Adapter(
            requireNotNull(activity),
            binding.cameraView.previewFrame,
            MINIMUM_RESOLUTION,
            DefaultCameraErrorListener(requireNotNull(activity)) { cause ->
                Log.e(TAG, "scan fails with exception: $cause")
            }
        )

        cameraPermissionEnsureable.ensureCameraPermission(
            onCameraReady = {
                Log.d(TAG, "Camera permission granted")
                cameraViewModel.targetScanType = ID_FRONT
                startScanning(ID_FRONT)
            },

            onUserDeniedCameraPermission = {
                Log.d(TAG, "Camera permission denied")
                findNavController().navigate(
                    R.id.action_camera_permission_denied,
                    bundleOf(
                        CameraPermissionDeniedFragment.ARG_SCAN_TYPE to ID_FRONT
                    )
                )
            }
        )
    }

    /**
     * Start scanning for the required scan type.
     */
    private fun startScanning(scanType: IdentityScanState.ScanType) {
        cameraAdapter.bindToLifecycle(this)
        // TODO(ccen): pack this logic into a IdentitySpecific CameraViewModel
        cameraViewModel.scanState = null
        cameraViewModel.scanStatePrevious = null
        cameraViewModel.identityScanFlow.startFlow(
            context = requireContext(),
            imageStream = cameraAdapter.getImageStream(),
            viewFinder = binding.cameraView.viewFinderWindowView.asRect(),
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

    private fun updateUI(identityState: IdentityScanState) {
        when (identityState) {
            is IdentityScanState.Initial -> {
                cameraView.viewFinderBackgroundView.visibility = View.VISIBLE
                cameraView.viewFinderWindowView.visibility = View.VISIBLE
                cameraView.viewFinderBorderView.visibility = View.VISIBLE
                continueButton.isEnabled = false
                checkMarkView.visibility = View.GONE
                when (cameraViewModel.targetScanType) {
                    ID_FRONT -> {
                        headerTitle.text = requireContext().getText(R.string.front_of_id)
                        messageView.text = requireContext().getText(R.string.position_id_front)
                    }
                    ID_BACK -> {
                        headerTitle.text = requireContext().getText(R.string.back_of_id)
                        messageView.text = requireContext().getText(R.string.position_id_back)
                    }
                    else -> {
                        Log.e(
                            TAG,
                            "Incorrect target scan type: ${cameraViewModel.targetScanType}"
                        )
                    }
                }

                cameraView.viewFinderWindowView.setBackgroundResource(R.drawable.id_viewfinder_background)
                cameraView.viewFinderBorderView.startAnimation(R.drawable.id_border_initial)
            }
            is IdentityScanState.Found -> {
                messageView.text = requireContext().getText(R.string.hold_still)
                cameraView.viewFinderWindowView.setBackgroundResource(R.drawable.id_viewfinder_background)
                cameraView.viewFinderBorderView.startAnimation(R.drawable.id_border_found)
            }
            is IdentityScanState.Unsatisfied -> {
                when (cameraViewModel.targetScanType) {
                    ID_FRONT -> {
                        messageView.text = requireContext().getText(R.string.position_id_front)
                    }
                    ID_BACK -> {
                        messageView.text = requireContext().getText(R.string.position_id_back)
                    }
                    else -> {
                        Log.e(
                            TAG,
                            "Incorrect target scan type: ${cameraViewModel.targetScanType}"
                        )
                    }
                }
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
                continueButton.isEnabled = true

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
