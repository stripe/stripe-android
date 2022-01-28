package com.stripe.android.stripecardscan.scanui

import android.util.Log
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener
import com.stripe.android.stripecardscan.framework.Config

internal interface SimpleScanStateful {

    var scanStatePrevious: ScanState?
    var scanState: ScanState
    val scanErrorListener: ScanErrorListener

    /**
     * The state of the scan flow. This can be expanded if [displayState] is overridden to handle
     * the added states.
     */
    abstract class ScanState(val isFinal: Boolean) {
        object NotFound : ScanState(isFinal = false)
        object Found : ScanState(isFinal = false)
        object Correct : ScanState(isFinal = true)
        object Wrong : ScanState(isFinal = false)
    }

    fun ensureValidParams(): Boolean

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

internal class ScanErrorListener : AnalyzerLoopErrorListener {
    override fun onAnalyzerFailure(t: Throwable): Boolean {
        Log.e(Config.logTag, "Error executing analyzer", t)
        return false
    }

    override fun onResultFailure(t: Throwable): Boolean {
        Log.e(Config.logTag, "Error executing result", t)
        return true
    }
}
