package com.stripe.android.identity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener
import com.stripe.android.identity.camera.IDDetectorAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.states.ScanState

internal class IdentityViewModel(
    val args: IdentityVerificationSheetContract.Args
) :
    ViewModel(),
    AnalyzerLoopErrorListener,
    AggregateResultListener<IDDetectorAggregator.InterimResult, IDDetectorAggregator.FinalResult> {

    internal val identityScanFlow = IdentityScanFlow(
        // TODO(ccen) Pass the correct scan type parameter after moved to separate Fragments
        scanType = ScanState.ScanType.ID_FRONT,
        this,
        this
    )

    override suspend fun onResult(result: IDDetectorAggregator.FinalResult) {
        Log.d(TAG, "Final result received: $result")
    }

    override suspend fun onInterimResult(result: IDDetectorAggregator.InterimResult) {
        Log.d(TAG, "Interim result received: $result")
    }

    override suspend fun onReset() {
        Log.d(TAG, "onReset is called, resetting status")
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
