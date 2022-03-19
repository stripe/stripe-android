package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanState.ScanType.ID_BACK
import com.stripe.android.identity.states.IdentityScanState.ScanType.ID_FRONT

/**
 * Fragment to scan the ID.
 */
internal class IDScanFragment(
    identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityCameraScanFragment(
    identityCameraScanViewModelFactory,
    identityViewModelFactory
) {
    override val headerTitleRes = R.string.front_of_id
    override val messageRes = R.string.position_id_front

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        continueButton.setOnClickListener {
            when (identityScanViewModel.targetScanType) {
                ID_FRONT -> {
                    startScanning(ID_BACK)
                }
                ID_BACK -> {
                    observeAndUploadForBothSides(IdDocumentParam.Type.IDCARD)
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
        startScanning(ID_FRONT)
    }

    override fun updateUI(identityScanState: IdentityScanState) {
        super.updateUI(identityScanState)
        when (identityScanState) {
            is IdentityScanState.Initial -> {
                when (identityScanViewModel.targetScanType) {
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
                            "Incorrect target scan type: ${identityScanViewModel.targetScanType}"
                        )
                    }
                }
            }
            is IdentityScanState.Unsatisfied -> {
                when (identityScanViewModel.targetScanType) {
                    ID_FRONT -> {
                        messageView.text = requireContext().getText(R.string.position_id_front)
                    }
                    ID_BACK -> {
                        messageView.text = requireContext().getText(R.string.position_id_back)
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
        val TAG: String = IDScanFragment::class.java.simpleName
    }
}
