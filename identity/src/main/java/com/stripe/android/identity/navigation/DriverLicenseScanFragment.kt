package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanState.ScanType.DL_BACK
import com.stripe.android.identity.states.IdentityScanState.ScanType.DL_FRONT

/**
 * Fragment to scan the Driver's license.
 */
internal class DriverLicenseScanFragment(
    identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityCameraScanFragment(
    identityCameraScanViewModelFactory,
    identityViewModelFactory
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        continueButton.setOnClickListener {
            when (identityScanViewModel.targetScanType) {
                DL_FRONT -> {
                    startScanning(DL_BACK)
                }
                DL_BACK -> {
                    observeAndUploadForBothSides(IdDocumentParam.Type.DRIVINGLICENSE)
                }
                else -> {
                    Log.e(
                        TAG,
                        "Incorrect target scan type: ${identityScanViewModel.targetScanType}"
                    )
                }
            }
        }
    }

    override fun onCameraReady() {
        if (shouldStartFromBack()) {
            startScanning(DL_BACK)
        } else {
            startScanning(DL_FRONT)
        }
    }

    override fun updateUI(identityScanState: IdentityScanState) {
        super.updateUI(identityScanState)
        when (identityScanState) {
            is IdentityScanState.Initial -> {
                when (identityScanViewModel.targetScanType) {
                    DL_FRONT -> {
                        headerTitle.text = requireContext().getText(R.string.front_of_dl)
                        messageView.text = requireContext().getText(R.string.position_dl_front)
                    }
                    DL_BACK -> {
                        headerTitle.text = requireContext().getText(R.string.back_of_dl)
                        messageView.text = requireContext().getText(R.string.position_dl_back)
                    }
                    else -> {
                        Log.e(
                            TAG,
                            "Incorrect target scan type: ${identityScanViewModel.targetScanType}"
                        )
                    }
                }
            }
            is IdentityScanState.Unsatisfied -> {
                when (identityScanViewModel.targetScanType) {
                    DL_FRONT -> {
                        messageView.text = requireContext().getText(R.string.position_dl_front)
                    }
                    DL_BACK -> {
                        messageView.text = requireContext().getText(R.string.position_dl_back)
                    }
                    else -> {
                        Log.e(
                            TAG,
                            "Incorrect target scan type: ${identityScanViewModel.targetScanType}"
                        )
                    }
                }
            }
            else -> {} // no-op
        }
    }

    private companion object {
        val TAG: String = DriverLicenseScanFragment::class.java.simpleName
    }
}
