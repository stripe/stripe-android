package com.stripe.android.identity.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.identity.analytics.ModelPerformanceTracker
import com.stripe.android.identity.states.IdentityScanState
import javax.inject.Inject

internal class IdentityScanViewModel(modelPerformanceTracker: ModelPerformanceTracker) :
    CameraViewModel(modelPerformanceTracker) {

    /**
     * The target ScanType of current scan.
     */
    internal var targetScanType: IdentityScanState.ScanType? = null

    internal class IdentityScanViewModelFactory @Inject constructor(
        private val modelPerformanceTracker: ModelPerformanceTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdentityScanViewModel(modelPerformanceTracker) as T
        }
    }
}
