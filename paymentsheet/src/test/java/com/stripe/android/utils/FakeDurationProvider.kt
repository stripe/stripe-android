package com.stripe.android.utils

import com.stripe.android.core.utils.DurationProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FakeDurationProvider(
    private val originalDuration: Duration = 1.seconds,
) : DurationProvider {

    private var overrideDuration: Duration? = null

    fun enqueueDuration(duration: Duration?) {
        overrideDuration = duration
    }

    override fun start(key: DurationProvider.Key) {
        // Nothing to do here
    }

    override fun end(key: DurationProvider.Key): Duration {
        val result = overrideDuration ?: originalDuration
        overrideDuration = null
        return result
    }
}
