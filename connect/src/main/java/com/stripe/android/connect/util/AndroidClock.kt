package com.stripe.android.connect.util

/**
 * [Clock] interface to be used to provide compatible `Clock` functionality,
 * and one day be replaced by `java.time.Clock` when all consumers support > SDK 26.
 *
 * Also useful for mocking in tests.
 */
internal interface Clock {

    /**
     * Return the current system time in milliseconds
     */
    fun millis(): Long
}

/**
 * A [Clock] that depends on Android APIs. To be replaced by java.time.Clock when all consumers
 * support > SDK 26.
 */
internal class AndroidClock : Clock {
    override fun millis(): Long = System.currentTimeMillis()
}
