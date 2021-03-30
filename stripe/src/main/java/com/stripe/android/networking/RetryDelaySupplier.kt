package com.stripe.android.networking

import java.util.concurrent.TimeUnit
import kotlin.math.pow

internal class RetryDelaySupplier(
    private val incrementSeconds: Long = DEFAULT_INCREMENT_SECONDS
) {
    /**
     * Calculate an exponential backoff delay before retrying the next completion request
     * using the equation:
     * ```
     * incrementSeconds ^ ((maxRetries - remainingRetries) + 1)
     * ```
     *
     * For example, if [maxRetries] is 3:
     * - Delay 2 seconds before the first retry
     * - Delay 4 seconds before the second retry
     * - Delay 8 seconds before the third retry
     */
    fun getDelayMillis(
        maxRetries: Int,
        remainingRetries: Int
    ): Long {
        val retryAttempt = maxRetries - remainingRetries.coerceIn(1, maxRetries) + 1
        return TimeUnit.SECONDS.toMillis(
            incrementSeconds.toDouble().pow(retryAttempt).toLong()
        )
    }

    private companion object {
        private const val DEFAULT_INCREMENT_SECONDS = 2L
    }
}
