package com.stripe.android.stripecardscan.framework

import android.util.Log
import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.ClockMark

internal abstract class MachineState {

    /**
     * Keep track of when this state was reached
     */
    protected open val reachedStateAt: ClockMark = Clock.markNow()

    override fun toString(): String =
        "${this::class.java.simpleName}(reachedStateAt=$reachedStateAt)"

    init {
        Log.d(LOG_TAG, "${this::class.java.simpleName} machine state reached")
    }
}
