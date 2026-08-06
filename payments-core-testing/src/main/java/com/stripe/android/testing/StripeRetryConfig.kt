package com.stripe.android.testing

object StripeRetryConfig {
    private const val DEFAULT_ATTEMPT_COUNT = 1

    private var retryEnabled: Boolean = false
    private var maxAttempts: Int = 1

    fun set(retryCount: Int, enabled: Boolean) {
        retryEnabled = enabled

        val retryCount = retryCount.takeIf { retryEnabled } ?: DEFAULT_ATTEMPT_COUNT

        maxAttempts = retryCount.coerceAtLeast(1)
    }

    fun shouldRetry(): Boolean = retryEnabled && maxAttempts > 1

    fun maxAttempts(): Int = maxAttempts
}
