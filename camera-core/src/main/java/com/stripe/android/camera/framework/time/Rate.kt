package com.stripe.android.camera.framework.time

import kotlin.time.Duration

/**
 * A rate of execution.
 */
internal data class Rate(val amount: Long, val duration: Duration) : Comparable<Rate> {
    override fun compareTo(other: Rate): Int {
        return (other.duration.inWholeNanoseconds.toDouble() / other.amount)
            .compareTo(duration.inWholeNanoseconds.toDouble() / amount)
    }
}
