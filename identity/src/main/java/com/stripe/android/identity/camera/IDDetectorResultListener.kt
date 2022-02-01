package com.stripe.android.identity.camera

import android.util.Log
import com.stripe.android.camera.framework.AggregateResultListener

internal class IDDetectorResultListener :
    AggregateResultListener<IDDetectorAggregator.InterimResult, IDDetectorAggregator.FinalResult> {
    override suspend fun onResult(result: IDDetectorAggregator.FinalResult) {
        Log.d(TAG, "IDDetectorResultListener::onResult: $result")
    }

    override suspend fun onInterimResult(result: IDDetectorAggregator.InterimResult) {
        Log.d(TAG, "IDDetectorResultListener::onInterimResult: $result")

    }

    override suspend fun onReset() {
        Log.d(TAG, "IDDetectorResultListener::onReset")
    }

    internal companion object {
        val TAG: String = IDDetectorResultListener::class.java.simpleName
    }
}
