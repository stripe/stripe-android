package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinearRetryDelaySupplier(
    private val delay: Duration
) : RetryDelaySupplier {

    @Inject
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
