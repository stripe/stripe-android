package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinearRetryDelaySupplier(
    private val delay: Long
) : RetryDelaySupplier {

    @Inject
    constructor() : this(DEFAULT_DELAY)

    /**
     * Gets a linear delay time regardless of provided parameters
     *
     * @return static delay in milliseconds
     */
    override fun getDelayMillis(
        maxRetries: Int,
        remainingRetries: Int
    ): Long {
        return TimeUnit.SECONDS.toMillis(delay)
    }

    private companion object {
        private const val DEFAULT_DELAY = 3L
    }
}
