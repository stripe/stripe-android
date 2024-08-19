package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import kotlin.time.Duration

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface RetryDelaySupplier {

    /**
     * Returns the max duration if all retries are attempted.
     */
    fun maxDuration(maxRetries: Int): Duration

    /**
     * Gets a delay based on the max retries and remaining retries
     *
     * @param maxRetries maximum amount of available retries
     * @param remainingRetries remaining number of retries available
     *
     * @return new delay as a [Duration] object
     */
    fun getDelay(
        maxRetries: Int,
        remainingRetries: Int
    ): Duration
}
