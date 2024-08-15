package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExponentialBackoffRetryDelaySupplier(
    private val incrementDuration: Duration
) : RetryDelaySupplier {

    @Inject
    constructor() : this(DEFAULT_INCREMENT_SECONDS.toDuration(DurationUnit.SECONDS))

    override fun maxDuration(maxRetries: Int): Duration {
        var resultDuration = 0.seconds
        for (i in maxRetries downTo 1) {
            resultDuration = resultDuration.plus(getDelay(maxRetries = maxRetries, remainingRetries = i))
        }
        return resultDuration
    }

    /**
     * Calculate an exponential backoff delay before retrying the next completion request
     * using the equation:
     * ```
     * incrementDuration ^ ((maxRetries - remainingRetries) + 1)
     * ```
     *
     * For example, if [maxRetries] is 3 and [incrementDuration] is 2 seconds:
     * - Delay 2 seconds before the first retry
     * - Delay 4 seconds before the second retry
     * - Delay 8 seconds before the third retry
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    override fun getDelay(
        maxRetries: Int,
        remainingRetries: Int
    ): Duration {
        val retryAttempt = maxRetries - remainingRetries.coerceIn(1, maxRetries) + 1

        return incrementDuration
            .toDouble(DurationUnit.SECONDS)
            .pow(retryAttempt)
            .toDuration(DurationUnit.SECONDS)
    }

    private companion object {
        private const val DEFAULT_INCREMENT_SECONDS = 2L
    }
}
