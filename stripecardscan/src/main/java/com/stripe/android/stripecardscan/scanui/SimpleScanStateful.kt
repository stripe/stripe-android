package com.stripe.android.stripecardscan.scanui

import android.util.Log
import androidx.annotation.RestrictTo
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener
import com.stripe.android.stripecardscan.framework.Config

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class ScanState(val isFinal: Boolean)

internal interface SimpleScanStateful<State : ScanState> {

    var scanStatePrevious: ScanState?
    var scanState: ScanState
    val scanErrorListener: ScanErrorListener

    fun displayState(newState: ScanState, previousState: ScanState?)

    fun changeScanState(newState: ScanState): Boolean {
        if (newState == scanStatePrevious || scanStatePrevious?.isFinal == true) {
            return false
        }

        scanState = newState
        displayState(newState, scanStatePrevious)
        scanStatePrevious = newState
        return true
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ScanErrorListener : AnalyzerLoopErrorListener {
    override fun onAnalyzerFailure(t: Throwable): Boolean {
        Log.e(Config.logTag, "Error executing analyzer", t)
        return false
    }

    override fun onResultFailure(t: Throwable): Boolean {
        Log.e(Config.logTag, "Error executing result", t)
        return true
    }
}
