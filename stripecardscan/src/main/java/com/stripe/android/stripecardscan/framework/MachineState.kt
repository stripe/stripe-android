package com.stripe.android.stripecardscan.framework

import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal abstract class MachineState(internal val timeSource: TimeSource) {

    /**
     * Keep track of when this state was reached
     */
    protected open val reachedStateAt: TimeMark = timeSource.markNow()
}
