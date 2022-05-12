package com.stripe.android.identity.navigation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.camera.Camera1Adapter
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.SelfieScanFragmentBinding
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.LoadingButton
import com.stripe.android.identity.ui.SelfieResultAdapter

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

    private val selfieResultAdapter = SelfieResultAdapter()

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
            // TODO(ccen) collect 3 images from ML pipe line and upload them
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO(ccen) track identityScanViewModel.finalResult and upload processed images
    }

    /**
     * Collect the 3 results from ML pipe line.
     *
     * TODO(ccen): replace the place holder with bitmap from ML pipe line.
     */
    private fun getResults(): List<Bitmap> {
        return listOf()
    }

    override fun createCameraAdapter(): Camera1Adapter {
        return Camera1Adapter(
            requireNotNull(activity),
            cameraView.previewFrame,
            MINIMUM_RESOLUTION,
            DefaultCameraErrorListener(requireNotNull(activity)) { cause ->
                Log.e(TAG, "scan fails with exception: $cause")
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
                resetUI()
            }
            is IdentityScanState.Found -> {
                binding.message.text = requireContext().getText(R.string.capturing)
            }
            is IdentityScanState.Unsatisfied -> {} // no-op
            is IdentityScanState.Satisfied -> {
                binding.message.text = requireContext().getText(R.string.selfie_capture_complete)
            }
            is IdentityScanState.Finished -> {
                flash()
                // TODO(ccen) - get result from identityScanState
                toggleResultViewWithResult(getResults())
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
     * Animate the selfie view with a flash.
     */
    private fun flash() {
        flashAnimatorSet.start()
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
