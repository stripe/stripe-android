package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.camera.scanui.util.startAnimation
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.DriverLicenseScanFragmentBinding
import com.stripe.android.identity.states.IdentityScanState

/**
 * Fragment to scan the Driver's license.
 */
internal class DriverLicenseScanFragment(
    cameraPermissionEnsureable: CameraPermissionEnsureable,
    cameraViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityCameraScanFragment(
    cameraPermissionEnsureable,
    cameraViewModelFactory,
    identityViewModelFactory
) {
    private lateinit var binding: DriverLicenseScanFragmentBinding
    private lateinit var headerTitle: TextView
    private lateinit var messageView: TextView
    private lateinit var checkMarkView: ImageView
    private lateinit var continueButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DriverLicenseScanFragmentBinding.inflate(inflater, container, false)
        cameraView = binding.cameraView

        headerTitle = binding.headerTitle
        messageView = binding.message
        checkMarkView = binding.checkMarkView
        continueButton = binding.kontinue
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        continueButton.setOnClickListener {
            when (cameraViewModel.targetScanType) {
                IdentityScanState.ScanType.DL_FRONT -> {
                    startScanning(IdentityScanState.ScanType.DL_BACK)
                    cameraViewModel.targetScanType = IdentityScanState.ScanType.DL_BACK
                }
                IdentityScanState.ScanType.DL_BACK -> {
                    findNavController().navigate(R.id.action_driverLicenseScanFragment_to_confirmationFragment)
                }
                else -> {
                    Log.e(TAG, "Incorrect target scan type: ${cameraViewModel.targetScanType}")
                }
            }
        }
    }

    override fun onCameraReady() {
        cameraViewModel.targetScanType = IdentityScanState.ScanType.DL_FRONT
        startScanning(IdentityScanState.ScanType.DL_FRONT)
    }

    override fun onUserDeniedCameraPermission() {
        findNavController().navigate(
            R.id.action_camera_permission_denied,
            bundleOf(
                CameraPermissionDeniedFragment.ARG_SCAN_TYPE to IdentityScanState.ScanType.DL_FRONT
            )
        )
    }

    override fun updateUI(identityScanState: IdentityScanState) {
        when (identityScanState) {
            is IdentityScanState.Initial -> {
                cameraView.viewFinderBackgroundView.visibility = View.VISIBLE
                cameraView.viewFinderWindowView.visibility = View.VISIBLE
                cameraView.viewFinderBorderView.visibility = View.VISIBLE
                continueButton.isEnabled = false
                checkMarkView.visibility = View.GONE
                when (cameraViewModel.targetScanType) {
                    IdentityScanState.ScanType.DL_FRONT -> {
                        headerTitle.text = requireContext().getText(R.string.front_of_dl)
                        messageView.text = requireContext().getText(R.string.position_dl_front)
                    }
                    IdentityScanState.ScanType.DL_BACK -> {
                        headerTitle.text = requireContext().getText(R.string.back_of_dl)
                        messageView.text = requireContext().getText(R.string.position_dl_back)
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
                    IdentityScanState.ScanType.DL_FRONT -> {
                        messageView.text = requireContext().getText(R.string.position_dl_front)
                    }
                    IdentityScanState.ScanType.DL_BACK -> {
                        messageView.text = requireContext().getText(R.string.position_dl_back)
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

    private companion object {
        val TAG: String = DriverLicenseScanFragment::class.java.simpleName
    }
}
