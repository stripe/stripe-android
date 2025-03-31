package com.stripe.android.core.utils

import android.os.SystemClock
import androidx.annotation.RestrictTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface DurationProvider {
    fun start(key: Key, reset: Boolean = true)
    fun end(key: Key): Duration?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Key {
        Loading,
        Checkout,
        LinkSignup,
        ConfirmButtonClicked,
        CardScan
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultDurationProvider private constructor() : DurationProvider {

    private val store = mutableMapOf<DurationProvider.Key, Long>()

    override fun start(key: DurationProvider.Key, reset: Boolean) {
        if (reset || key !in store) {
            val startTime = SystemClock.uptimeMillis()
            store[key] = startTime
        }
    }

    override fun end(key: DurationProvider.Key): Duration? {
        val startTime = store.remove(key) ?: return null
        val duration = SystemClock.uptimeMillis() - startTime
        return duration.milliseconds
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        val instance = DefaultDurationProvider()
    }
}
