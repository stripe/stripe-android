package com.stripe.android.core.networking

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface RetryDelaySupplier {

    /**
     * Gets a delay in milliseconds based on the max retries and remaining retries
     *
     * @param maxRetries maximum amount of available retries
     * @param remainingRetries remaining number of retries available
     *
     * @return new delay in milliseconds
     */
    fun getDelayMillis(
        maxRetries: Int,
        remainingRetries: Int
    ): Long
}
