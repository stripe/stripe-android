package com.stripe.android.identity.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.core.injection.UIContext
import com.stripe.android.identity.analytics.ModelPerformanceTracker
import com.stripe.android.identity.states.IdentityScanState
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class IdentityScanViewModel(
    modelPerformanceTracker: ModelPerformanceTracker,
    @UIContext private val uiContext: CoroutineContext
) :
    CameraViewModel(modelPerformanceTracker, uiContext) {

    /**
     * The target ScanType of current scan.
     * TODO(ccen) remove after SelfieFragment is migrated to JetpackCompose.
     */
    internal var targetScanType: IdentityScanState.ScanType? = null

    /**
     * StateFlow to keep track of current target scan type.
     */
    internal val targetScanTypeFlow = MutableStateFlow<IdentityScanState.ScanType?>(null)

    internal class IdentityScanViewModelFactory @Inject constructor(
        private val modelPerformanceTracker: ModelPerformanceTracker,
        @UIContext private val uiContext: CoroutineContext
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdentityScanViewModel(modelPerformanceTracker, uiContext) as T
        }
    }
}
