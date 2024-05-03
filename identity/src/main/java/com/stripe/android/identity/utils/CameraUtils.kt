package com.stripe.android.identity.utils

import androidx.lifecycle.LifecycleOwner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel

internal fun startScanning(
    scanType: IdentityScanState.ScanType,
    identityViewModel: IdentityViewModel,
    identityScanViewModel: IdentityScanViewModel,
    lifecycleOwner: LifecycleOwner
) {
    identityViewModel.updateNewScanType(scanType)
    identityScanViewModel.fpsTracker.start()
    identityScanViewModel.startScan(
        scanType = scanType,
        lifecycleOwner = lifecycleOwner
    )
}
