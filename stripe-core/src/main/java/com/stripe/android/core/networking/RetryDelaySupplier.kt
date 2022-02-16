package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RetryDelaySupplier(
    private val incrementSeconds: Long
) {

    @Inject
    constructor() : this(DEFAULT_INCREMENT_SECONDS)

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
