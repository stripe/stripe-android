package com.stripe.android.identity.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.IdentityViewModel
import com.stripe.android.identity.camera.IDDetectorAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.states.ScanState

/**
 * ViewModel hosted by all fragments that need to access live camera feed and callbacks.
 */
internal class CameraViewModel :
    ViewModel(),
    AnalyzerLoopErrorListener,
    AggregateResultListener<IDDetectorAggregator.InterimResult, IDDetectorAggregator.FinalResult> {

    // liveData for results, subscribed from fragments
    internal val interimResults = MutableLiveData<IDDetectorAggregator.InterimResult>()
    internal val finalResult = MutableLiveData<IDDetectorAggregator.FinalResult>()
    internal val reset = MutableLiveData<Unit>()

    lateinit var identityScanFlow: IdentityScanFlow

    /**
     * Initialize [identityScanFlow] with the target scanType.
     *
     * TODO(ccen): Extract scanType from [IdentityScanFlow]'s constructor, initialize the scan flow
     * upon [CameraViewModel]'s initialization, add the ability to update scanType of a
     * [IdentityScanFlow] on the fly.
     */
    fun initializeScanFlow(
        scanType: ScanState.ScanType
    ) {
        identityScanFlow = IdentityScanFlow(
            scanType = scanType,
            this,
            this
        )
    }

    override suspend fun onResult(result: IDDetectorAggregator.FinalResult) {
        Log.d(TAG, "Final result received: $result")
        finalResult.postValue(result)
    }

    override suspend fun onInterimResult(result: IDDetectorAggregator.InterimResult) {
        Log.d(TAG, "Interim result received: $result")
        interimResults.postValue(result)
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

    internal class IdentityViewModelFactory(
        private val args: IdentityVerificationSheetContract.Args
    ) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdentityViewModel(args) as T
        }
    }

    private companion object {
        val TAG: String = IdentityViewModel::class.java.simpleName
    }
}
