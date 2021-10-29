package com.stripe.android.cardverificationsheet.framework.time

/**
 * A rate of execution.
 */
data class Rate(val amount: Long, val duration: Duration) : Comparable<Rate> {
    override fun compareTo(other: Rate): Int {
        return (other.duration / other.amount).compareTo(duration / amount)
    }
}
