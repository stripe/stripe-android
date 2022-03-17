package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.scanui.util.startAnimation
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.PassportScanFragmentBinding
import com.stripe.android.identity.states.IdentityScanState

/**
 * Fragment to scan passport.
 */
internal class PassportScanFragment(
    cameraViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityCameraScanFragment(
    cameraViewModelFactory,
    identityViewModelFactory
) {
    private lateinit var binding: PassportScanFragmentBinding
    private lateinit var headerTitle: TextView
    private lateinit var messageView: TextView

    private lateinit var checkMarkView: ImageView
    private lateinit var continueButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PassportScanFragmentBinding.inflate(inflater, container, false)
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
            findNavController().navigate(R.id.action_passportScanFragment_to_confirmationFragment)
        }
    }

    override fun onCameraReady() {
        cameraViewModel.targetScanType = IdentityScanState.ScanType.PASSPORT
        startScanning(IdentityScanState.ScanType.PASSPORT)
    }

    override fun updateUI(identityScanState: IdentityScanState) {
        when (identityScanState) {
            is IdentityScanState.Initial -> {
                cameraView.viewFinderBackgroundView.visibility = View.VISIBLE
                cameraView.viewFinderWindowView.visibility = View.VISIBLE
                cameraView.viewFinderBorderView.visibility = View.VISIBLE
                continueButton.isEnabled = false
                checkMarkView.visibility = View.GONE

                headerTitle.text = requireContext().getText(R.string.passport)

                cameraView.viewFinderWindowView.setBackgroundResource(R.drawable.id_viewfinder_background)
                cameraView.viewFinderBorderView.startAnimation(R.drawable.id_border_initial)
            }
            is IdentityScanState.Found -> {
                messageView.text = requireContext().getText(R.string.hold_still)
                cameraView.viewFinderWindowView.setBackgroundResource(R.drawable.id_viewfinder_background)
                cameraView.viewFinderBorderView.startAnimation(R.drawable.id_border_found)
            }
            is IdentityScanState.Unsatisfied -> {
                messageView.text = requireContext().getText(R.string.position_passport)
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
}
