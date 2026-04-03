package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class LinearRetryDelaySupplier(
    private val delay: Duration
) : RetryDelaySupplier {

    constructor() : this(DEFAULT_DELAY.toDuration(DurationUnit.SECONDS))

    override fun maxDuration(maxRetries: Int): Duration {
        return delay.times(maxRetries)
    }

    /**
     * Gets a linear delay time regardless of provided parameters
     *
     * @return static delay as a [Duration] object
     */
    override fun getDelay(
        maxRetries: Int,
        remainingRetries: Int
    ): Duration {
        return delay
    }

    private companion object {
        private const val DEFAULT_DELAY = 3L
    }
}
