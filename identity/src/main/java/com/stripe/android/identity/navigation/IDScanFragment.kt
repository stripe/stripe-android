package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.CollectedDataParam
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
    override val fragmentId = R.id.IDScanFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (shouldStartFromBack()) {
            headerTitle.text = requireContext().getText(R.string.back_of_id)
            messageView.text = requireContext().getText(R.string.position_id_back)
        } else {
            headerTitle.text = requireContext().getText(R.string.front_of_id)
            messageView.text = requireContext().getText(R.string.position_id_front)
        }

        continueButton.setOnClickListener {
            when (identityScanViewModel.targetScanType) {
                ID_FRONT -> {
                    startScanning(ID_BACK)
                }
                ID_BACK -> {
                    continueButton.toggleToLoading()
                    collectUploadedStateAndUploadForBothSides(CollectedDataParam.Type.IDCARD)
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
            startScanning(ID_BACK)
        } else {
            startScanning(ID_FRONT)
        }
    }

    override fun resetUI() {
        super.resetUI()
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

    private companion object {
        val TAG: String = IDScanFragment::class.java.simpleName
    }
}
