package com.stripe.android.identity.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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

    override fun displayState(newState: IdentityScanState, previousState: IdentityScanState?) {
        val stateKind = newState::class.simpleName
        val scanType = newState.type
        val feedbackRes = when (newState) {
            is IdentityScanState.Initial -> newState.feedbackRes
            is IdentityScanState.Found -> newState.feedbackRes
            else -> null
        }
        val unsatisfiedReason = (newState as? IdentityScanState.Unsatisfied)?.reason

        Log.i(
            TAG,
            "displayState: kind=" + stateKind +
                ", scanType=" + scanType +
                ", feedbackRes=" + feedbackRes +
                ", unsatisfiedReason=" + unsatisfiedReason +
                ", previousKind=" + previousState?.let { it::class.simpleName } +
                ", previousScanType=" + previousState?.type
        )

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
        Log.i(
            TAG,
            "startScan: scanType=" + scanType +
                ", hasCameraView=" + runCatching { cameraManager.requireCameraView() != null }
                .getOrDefault(false) +
                ", hasAdapter=" + (cameraManager.cameraAdapter != null) +
                ", flowInitialized=" + (identityScanFlow != null)
        )

        _scannerState.update { State.Scanning() }
        targetScanTypeFlow.update { scanType }

        runCatching {
            cameraManager.requireCameraAdapter().bindToLifecycle(lifecycleOwner)
        }.onSuccess {
            Log.i(TAG, "startScan: bindToLifecycle succeeded for scanType=" + scanType)
        }.onFailure { throwable ->
            Log.e(TAG, "startScan: bindToLifecycle FAILED for scanType=" + scanType, throwable)
        }

        cameraManager.toggleInitial()
        Log.i(TAG, "startScan: toggleInitial invoked for scanType=" + scanType)

        scanState = null
        scanStatePrevious = null

        try {
            if (identityScanFlow == null) {
                Log.e(TAG, "startScan: identityScanFlow is null, cannot start scan (scanType=" + scanType + ")")
            } else {
                identityScanFlow?.startFlow(
                    context = applicationContext,
                    imageStream = cameraManager.requireCameraAdapter().getImageStream(),
                    viewFinder = cameraManager.requireCameraView().viewFinderWindowView.asRect(),
                    lifecycleOwner = lifecycleOwner,
                    coroutineScope = viewModelScope,
                    parameters = scanType,
                    errorHandler = { e ->
                        Log.e(TAG, "startScan: identityScanFlow errorHandler invoked for scanType=" + scanType, e)
                        verificationFlowFinishable.finishWithResult(
                            IdentityVerificationSheet.VerificationFlowResult.Failed(e)
                        )
                    }
                )
                Log.i(TAG, "startScan: identityScanFlow.startFlow started for scanType=" + scanType)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "startScan: startFlow threw for scanType=" + scanType, t)
            throw t
        }
    }

    fun stopScan(lifecycleOwner: LifecycleOwner) {
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
        _scannerState.update { State.Scanning() }
    }

    fun resetScannerState() {
        _scannerState.update { State.Initializing }
    }

    private companion object {
        private const val TAG: String = "IdentityScanViewModel"
    }
}
