package com.stripe.android.paymentsheet.analytics

import android.os.SystemClock
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal interface DurationProvider {
    fun start(key: Key)
    fun end(key: Key): Duration?

    enum class Key {
        Checkout,
    }
}

internal class DefaultDurationProvider @Inject constructor() : DurationProvider {

    private val store = mutableMapOf<DurationProvider.Key, Long>()

    override fun start(key: DurationProvider.Key) {
        val startTime = SystemClock.uptimeMillis()
        store[key] = startTime
    }

    override fun end(key: DurationProvider.Key): Duration? {
        val startTime = store.remove(key) ?: return null
        val duration = SystemClock.uptimeMillis() - startTime
        return duration.milliseconds
    }
}
