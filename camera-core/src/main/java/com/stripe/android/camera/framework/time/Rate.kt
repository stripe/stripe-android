package com.stripe.android.camera.framework.time

/**
 * A rate of execution.
 */
internal data class Rate(val amount: Long, val duration: Duration) : Comparable<Rate> {
    override fun compareTo(other: Rate): Int {
        return (other.duration / other.amount).compareTo(duration / amount)
    }
}
