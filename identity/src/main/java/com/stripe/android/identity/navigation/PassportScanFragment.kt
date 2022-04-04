package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState

/**
 * Fragment to scan passport.
 */
internal class PassportScanFragment(
    identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityCameraScanFragment(
    identityCameraScanViewModelFactory,
    identityViewModelFactory
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        headerTitle.text = requireContext().getText(R.string.passport)
        messageView.text = requireContext().getText(R.string.position_passport)
        continueButton.setOnClickListener {
            observeAndUploadForFrontSide(CollectedDataParam.Type.PASSPORT)
        }
    }

    override fun onCameraReady() {
        startScanning(IdentityScanState.ScanType.PASSPORT)
    }

    override fun resetUI() {
        super.resetUI()
        headerTitle.text = requireContext().getText(R.string.passport)
        messageView.text = requireContext().getText(R.string.position_passport)
    }

    internal companion object {
        val TAG: String = PassportScanFragment::class.java.simpleName
    }
}
