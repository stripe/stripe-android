package com.stripe.android.stripecardscan.framework

import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.ClockMark

internal abstract class MachineState {

    /**
     * Keep track of when this state was reached
     */
    protected open val reachedStateAt: ClockMark = Clock.markNow()
}
