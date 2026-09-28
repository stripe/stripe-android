package com.stripe.android.payments

/** Supplies wall-clock time for payment flow polling. */
internal fun interface Clock {
    fun currentTimeMillis(): Long
}

internal object SystemClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
