package com.stripe.android.identity.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener
import com.stripe.android.camera.scanui.ScanErrorListener
import com.stripe.android.camera.scanui.SimpleScanStateful
import com.stripe.android.core.injection.UIContext
import com.stripe.android.identity.analytics.ModelPerformanceTracker
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.ml.FaceDetectorAnalyzer
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorAnalyzer
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.SingleLiveEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * ViewModel hosted by Activities/Fragments that need to access live camera feed and callbacks.
 *
 * TODO(ccen): Extract type parameters and move to camera-core
 */
internal open class CameraViewModel(
    private val modelPerformanceTracker: ModelPerformanceTracker,
    @UIContext private val uiContext: CoroutineContext
) :
    ViewModel(),
    AnalyzerLoopErrorListener,
    AggregateResultListener<IdentityAggregator.InterimResult, IdentityAggregator.FinalResult>,
    SimpleScanStateful<IdentityScanState> {
    internal val interimResults = MutableLiveData<IdentityAggregator.InterimResult>()
    internal val finalResult = SingleLiveEvent<IdentityAggregator.FinalResult>()
    private val reset = MutableLiveData<Unit>()

    // TODO(ccen): remove when SelfieFragment is also migrated to Jetpack Compose.
    internal val displayStateChanged =
        SingleLiveEvent<Pair<IdentityScanState, IdentityScanState?>>()

    internal val displayStateChangedFlow =
        MutableStateFlow<Pair<IdentityScanState, IdentityScanState?>?>(null)

    internal var identityScanFlow: IdentityScanFlow? = null

    internal fun initializeScanFlow(
        verificationPage: VerificationPage,
        idDetectorModelFile: File,
        faceDetectorModelFile: File?
    ) {
        identityScanFlow = IdentityScanFlow(
            this,
            this,
            idDetectorModelFile,
            faceDetectorModelFile,
            verificationPage,
            modelPerformanceTracker
        )
    }

    override var scanState: IdentityScanState? = null

    override var scanStatePrevious: IdentityScanState? = null

    override val scanErrorListener = ScanErrorListener()

    override fun displayState(newState: IdentityScanState, previousState: IdentityScanState?) {
        displayStateChanged.postValue(newState to previousState)
        displayStateChangedFlow.update {
            (newState to previousState)
        }
    }

    override suspend fun onResult(result: IdentityAggregator.FinalResult) {
        Log.d(TAG, "Final result received: $result")

        modelPerformanceTracker.reportAndReset(
            if (result.result is FaceDetectorOutput) {
                FaceDetectorAnalyzer.MODEL_NAME
            } else {
                IDDetectorAnalyzer.MODEL_NAME
            }
        )
        viewModelScope.launch(uiContext) {
            finalResult.value = result
        }
    }

    override suspend fun onInterimResult(result: IdentityAggregator.InterimResult) {
        Log.d(TAG, "Interim result received: $result")

        viewModelScope.launch(uiContext) {
            interimResults.value = result
        }
        // This will trigger displayState
        changeScanState(result.identityState)
    }

    override suspend fun onReset() {
        Log.d(TAG, "onReset is called, resetting status")
        scanState = null
        scanStatePrevious = null
        reset.postValue(Unit)
    }

    override fun onAnalyzerFailure(t: Throwable): Boolean {
        Log.d(TAG, "Error executing analyzer : $t, continue analyzing")
        return false
    }

    override fun onResultFailure(t: Throwable): Boolean {
        Log.d(TAG, "Error executing result : $t, stop analyzing")
        return true
    }

    private companion object {
        val TAG: String = CameraViewModel::class.java.simpleName
    }
}
