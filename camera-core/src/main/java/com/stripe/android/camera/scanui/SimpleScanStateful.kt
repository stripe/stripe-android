package com.stripe.android.camera.scanui

import android.util.Log
import androidx.annotation.RestrictTo
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class ScanState(val isFinal: Boolean)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface SimpleScanStateful<State : ScanState> {

    var scanStatePrevious: State?
    var scanState: State?
    val scanErrorListener: ScanErrorListener

    fun displayState(newState: State, previousState: State?)

    fun changeScanState(newState: State): Boolean {
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
        Log.e(TAG, "Error executing analyzer", t)
        return false
    }

    override fun onResultFailure(t: Throwable): Boolean {
        Log.e(TAG, "Error executing result", t)
        return true
    }

    private companion object {
        val TAG: String = SimpleScanStateful::class.java.simpleName
    }
}
