package com.stripe.android.identity.viewmodel

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.core.injection.UIContext
import com.stripe.android.identity.analytics.ModelPerformanceTracker
import com.stripe.android.identity.camera.IdentityCameraManager
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.LaplacianBlurDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class IdentityScanViewModel(
    private val context: WeakReference<Context>,
    modelPerformanceTracker: ModelPerformanceTracker,
    laplacianBlurDetector: LaplacianBlurDetector,
    @UIContext private val uiContext: CoroutineContext
) :
    CameraViewModel(modelPerformanceTracker, laplacianBlurDetector, uiContext) {

    /**
     * StateFlow to keep track of current target scan type.
     */
    internal val targetScanTypeFlow = MutableStateFlow<IdentityScanState.ScanType?>(null)

    private lateinit var cameraManager: IdentityCameraManager

    fun initializeCameraManager(cameraManager: IdentityCameraManager) {
        this.cameraManager = cameraManager
    }

    fun startScan(
        scanType: IdentityScanState.ScanType,
        lifecycleOwner: LifecycleOwner
    ) {
        targetScanTypeFlow.update { scanType }
        cameraManager.requireCameraAdapter().bindToLifecycle(lifecycleOwner)
        scanState = null
        scanStatePrevious = null

        identityScanFlow?.startFlow(
            context = requireNotNull(context.get()),
            imageStream = cameraManager.requireCameraAdapter().getImageStream(),
            viewFinder = cameraManager.requireCameraView().viewFinderWindowView.asRect(),
            lifecycleOwner = lifecycleOwner,
            coroutineScope = viewModelScope,
            parameters = scanType
        )
    }

    fun stopScan(lifecycleOwner: LifecycleOwner) {
        requireNotNull(identityScanFlow).resetFlow()
        cameraManager.requireCameraAdapter().unbindFromLifecycle(lifecycleOwner)
    }

    internal class IdentityScanViewModelFactory @Inject constructor(
        private val context: Context,
        private val modelPerformanceTracker: ModelPerformanceTracker,
        private val laplacianBlurDetector: LaplacianBlurDetector,
        @UIContext private val uiContext: CoroutineContext,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdentityScanViewModel(
                WeakReference(context),
                modelPerformanceTracker,
                laplacianBlurDetector,
                uiContext
            ) as T
        }
    }
}
