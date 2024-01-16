package com.stripe.android.identity.viewmodel

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.identity.analytics.FPSTracker
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.ModelPerformanceTracker
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.camera.IdentityCameraManager
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.LaplacianBlurDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.lang.ref.WeakReference
import javax.inject.Inject

internal class IdentityScanViewModel(
    private val context: WeakReference<Context>,
    val fpsTracker: FPSTracker,
    val identityRepository: IdentityRepository,
    val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory,
    modelPerformanceTracker: ModelPerformanceTracker,
    laplacianBlurDetector: LaplacianBlurDetector
) :
    CameraViewModel(modelPerformanceTracker, laplacianBlurDetector) {

    private val _scannerState: MutableStateFlow<State> = MutableStateFlow(State.Initializing)

    internal val scannerState: StateFlow<State> = _scannerState

    /**
     * StateFlow to keep track of current target scan type.
     */
    internal val targetScanTypeFlow = MutableStateFlow<IdentityScanState.ScanType?>(null)

    private lateinit var cameraManager: IdentityCameraManager

    internal sealed class State {
        object Initializing : State()
        class Scanning(
            val scanState: IdentityScanState? = null
        ) : State()

        class Scanned(val result: IdentityAggregator.FinalResult) : State()
        class Timeout(val fromSelfie: Boolean) : State()
    }

    override suspend fun onInterimResult(result: IdentityAggregator.InterimResult) {
        super.onInterimResult(result)
        fpsTracker.trackFrame()
    }

    override suspend fun onResult(result: IdentityAggregator.FinalResult) {
        super.onResult(result)
        if (result.identityState is IdentityScanState.Finished) {
            _scannerState.update { State.Scanned(result) }
        } else if (result.identityState is IdentityScanState.TimeOut) {
            _scannerState.update { State.Timeout(fromSelfie = result.result is FaceDetectorOutput) }
            when (result.result) {
                is FaceDetectorOutput -> {
                    identityRepository.sendAnalyticsRequest(
                        identityAnalyticsRequestFactory.selfieTimeout()
                    )
                }

                is IDDetectorOutput -> {
                    identityRepository.sendAnalyticsRequest(
                        identityAnalyticsRequestFactory.documentTimeout(
                            scanType = result.identityState.type
                        )
                    )
                }
            }
        }
        fpsTracker.reportAndReset(
            if (result.result is FaceDetectorOutput) {
                IdentityAnalyticsRequestFactory.TYPE_SELFIE
            } else {
                IdentityAnalyticsRequestFactory.TYPE_DOCUMENT
            }
        )
    }

    override fun displayState(newState: IdentityScanState, previousState: IdentityScanState?) {
        // toggle UX transition in CameraManager
        //  Done intentionally outside Jetpack Compose as cameraManger uses a traditional AndroidView
        when (newState) {
            is IdentityScanState.Initial -> {
                cameraManager.toggleInitial()
            }

            is IdentityScanState.Found -> {
                cameraManager.toggleFound()
            }

            is IdentityScanState.Satisfied -> {
                cameraManager.toggleSatisfied()
            }

            is IdentityScanState.Unsatisfied -> {
                cameraManager.toggleUnsatisfied()
            }

            is IdentityScanState.TimeOut -> {
                cameraManager.toggleTimeOut()
            }

            is IdentityScanState.Finished -> {
                cameraManager.toggleFinished()
            }
        }
        _scannerState.update { State.Scanning(newState) }
    }

    fun startScan(
        scanType: IdentityScanState.ScanType,
        lifecycleOwner: LifecycleOwner
    ) {
        _scannerState.update { State.Scanning() }
        targetScanTypeFlow.update { scanType }
        cameraManager.requireCameraAdapter().bindToLifecycle(lifecycleOwner)
        cameraManager.toggleInitial()
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

    /**
     * Initialize scanner, including [identityScanFlow] and [cameraManager].
     */
    fun initializeScanFlowAndUpdateState(
        pageAndModelFiles: IdentityViewModel.PageAndModelFiles,
        cameraManager: IdentityCameraManager
    ) {
        initializeScanFlow(
            pageAndModelFiles.page,
            idDetectorModelFile = pageAndModelFiles.idDetectorFile,
            faceDetectorModelFile = pageAndModelFiles.faceDetectorFile
        )
        this.cameraManager = cameraManager
        _scannerState.update { State.Scanning() }
    }

    fun resetScannerState() {
        _scannerState.update { State.Initializing }
    }

    internal class IdentityScanViewModelFactory @Inject constructor(
        private val context: Context,
        private val modelPerformanceTracker: ModelPerformanceTracker,
        private val laplacianBlurDetector: LaplacianBlurDetector,
        private val fpsTracker: FPSTracker,
        private val identityRepository: IdentityRepository,
        private val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdentityScanViewModel(
                WeakReference(context),
                fpsTracker,
                identityRepository,
                identityAnalyticsRequestFactory,
                modelPerformanceTracker,
                laplacianBlurDetector
            ) as T
        }
    }
}
