package com.stripe.android.identity.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener
import com.stripe.android.camera.scanui.ScanErrorListener
import com.stripe.android.camera.scanui.SimpleScanStateful
import com.stripe.android.identity.camera.IDDetectorAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.states.IdentityScanState
import java.io.File

/**
 * ViewModel hosted by Activities/Fragments that need to access live camera feed and callbacks.
 *
 * TODO(ccen): Extract type parameters and move to camera-core
 */
internal open class CameraViewModel :
    ViewModel(),
    AnalyzerLoopErrorListener,
    AggregateResultListener<IDDetectorAggregator.InterimResult, IDDetectorAggregator.FinalResult>,
    SimpleScanStateful<IdentityScanState> {
    private val interimResults = MutableLiveData<IDDetectorAggregator.InterimResult>()
    internal val finalResult = MutableLiveData<IDDetectorAggregator.FinalResult>()
    private val reset = MutableLiveData<Unit>()
    internal val displayStateChanged =
        MutableLiveData<Pair<IdentityScanState, IdentityScanState?>>()

    internal lateinit var identityScanFlow: IdentityScanFlow

    internal fun initializeScanFlow(identityModelFile: File) {
        identityScanFlow = IdentityScanFlow(this, this, identityModelFile)
    }

    override var scanState: IdentityScanState? = null

    override var scanStatePrevious: IdentityScanState? = null

    override val scanErrorListener = ScanErrorListener()

    override fun displayState(newState: IdentityScanState, previousState: IdentityScanState?) {
        displayStateChanged.postValue(newState to previousState)
    }

    override suspend fun onResult(result: IDDetectorAggregator.FinalResult) {
        Log.d(TAG, "Final result received: $result")
        finalResult.postValue(result)
    }

    override suspend fun onInterimResult(result: IDDetectorAggregator.InterimResult) {
        Log.d(TAG, "Interim result received: $result")
        interimResults.postValue(result)

        // This will trigger displayState
        changeScanState(result.identityState)
    }

    override suspend fun onReset() {
        Log.d(TAG, "onReset is called, resetting status")
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
