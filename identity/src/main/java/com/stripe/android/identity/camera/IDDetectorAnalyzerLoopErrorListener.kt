package com.stripe.android.identity.camera

import android.util.Log
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener

class IDDetectorAnalyzerLoopErrorListener : AnalyzerLoopErrorListener {
    override fun onAnalyzerFailure(t: Throwable): Boolean {
        Log.d(TAG, "onAnalyzerFailure : $t")
        return true
    }

    override fun onResultFailure(t: Throwable): Boolean {
        Log.d(TAG, "onResultFailure : $t")
        return true
    }

    companion object {
        val TAG: String = IDDetectorAnalyzerLoopErrorListener::class.java.simpleName
    }
}
