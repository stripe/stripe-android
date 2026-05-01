package com.stripe.android.identity.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.FPSTracker
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.ModelPerformanceTracker
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.camera.IdentityCameraManager
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.LaplacianBlurDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal abstract class IdentityScanViewModel(
    private val applicationContext: Application,
    open val fpsTracker: FPSTracker,
    open val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory,
    modelPerformanceTracker: ModelPerformanceTracker,
    laplacianBlurDetector: LaplacianBlurDetector,
    private val verificationFlowFinishable: VerificationFlowFinishable
) :
    CameraViewModel(
        modelPerformanceTracker,
        laplacianBlurDetector,
        identityAnalyticsRequestFactory
    ) {

    private val _scannerState: MutableStateFlow<State> = MutableStateFlow(State.Initializing)

    internal val scannerState: StateFlow<State> = _scannerState

    private val _latestManualCaptureFrame = MutableStateFlow<CameraPreviewImage<Bitmap>?>(null)
    internal val latestManualCaptureFrame: StateFlow<CameraPreviewImage<Bitmap>?> =
        _latestManualCaptureFrame

    private var manualCaptureFrameJob: Job? = null

    /**
     * StateFlow to keep track of current target scan type.
     */
    internal val targetScanTypeFlow = MutableStateFlow<IdentityScanState.ScanType?>(null)

    lateinit var cameraManager: IdentityCameraManager

    abstract val scanFeedback: StateFlow<Int?>

    internal sealed class State {
        data object Initializing : State()
        class Scanning(
            val scanState: IdentityScanState? = null
        ) : State()

        class Scanned(val result: IdentityAggregator.FinalResult) : State()
        data object ManualCaptured : State()
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
                    identityAnalyticsRequestFactory.selfieTimeout()
                }

                is IDDetectorOutput -> {
                    identityAnalyticsRequestFactory.documentTimeout(
                        scanType = result.identityState.type
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

    fun startManualCapture(
        scanType: IdentityScanState.ScanType,
        lifecycleOwner: LifecycleOwner
    ) {
        requireNotNull(identityScanFlow).resetFlow()
        resetManualCapture()
        _scannerState.update { State.Scanning() }
        targetScanTypeFlow.update { scanType }
        cameraManager.requireCameraAdapter().bindToLifecycle(lifecycleOwner)
        cameraManager.toggleInitial()
        manualCaptureFrameJob = viewModelScope.launch {
            cameraManager.requireCameraAdapter()
                .getImageStream()
                .collectLatest { frame ->
                    _latestManualCaptureFrame.update { frame }
                }
        }
    }

    fun captureManualResult(
        lifecycleOwner: LifecycleOwner
    ): CameraPreviewImage<Bitmap>? {
        val capturedFrame = latestManualCaptureFrame.value ?: return null
        stopScan(lifecycleOwner)
        _scannerState.update { State.ManualCaptured }
        return capturedFrame
    }

    override fun onAnalyzerFailure(t: Throwable): Boolean {
        identityAnalyticsRequestFactory.verificationFailed(
            isFromFallbackUrl = false,
            scanType = targetScanTypeFlow.value,
            throwable = t
        )

        verificationFlowFinishable.finishWithResult(
            IdentityVerificationSheet.VerificationFlowResult.Failed(
                t as? Exception ?: Exception(t)
            )
        )
        return true
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
        resetManualCapture()
        _scannerState.update { State.Scanning() }
        targetScanTypeFlow.update { scanType }
        cameraManager.requireCameraAdapter().bindToLifecycle(lifecycleOwner)
        cameraManager.toggleInitial()
        scanState = null
        scanStatePrevious = null

        identityScanFlow?.startFlow(
            context = applicationContext,
            imageStream = cameraManager.requireCameraAdapter().getImageStream(),
            viewFinder = cameraManager.requireCameraView().viewFinderWindowView.asRect(),
            lifecycleOwner = lifecycleOwner,
            coroutineScope = viewModelScope,
            parameters = scanType,
            errorHandler = { e ->
                verificationFlowFinishable.finishWithResult(
                    IdentityVerificationSheet.VerificationFlowResult.Failed(e)
                )
            }
        )
    }

    fun stopScan(lifecycleOwner: LifecycleOwner) {
        resetManualCapture()
        runCatching {
            requireNotNull(identityScanFlow).resetFlow()
            cameraManager.requireCameraAdapter().unbindFromLifecycle(lifecycleOwner)
        }.onFailure {
            identityAnalyticsRequestFactory.genericError("required object is null", it.stackTraceToString())
        }
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
        resetManualCapture()
        _scannerState.update { State.Scanning() }
    }

    fun resetScannerState() {
        resetManualCapture()
        _scannerState.update { State.Initializing }
    }

    private fun resetManualCapture() {
        manualCaptureFrameJob?.cancel()
        manualCaptureFrameJob = null
        _latestManualCaptureFrame.update { null }
    }
}
