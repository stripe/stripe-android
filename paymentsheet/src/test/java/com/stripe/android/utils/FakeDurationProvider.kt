package com.stripe.android.utils

import com.stripe.android.core.utils.DurationProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FakeDurationProvider(
    private val duration: Duration = 1.seconds,
) : DurationProvider {

    override fun start(key: DurationProvider.Key) {
        // Nothing to do here
    }

    override fun end(key: DurationProvider.Key): Duration {
        return duration
    }
}
