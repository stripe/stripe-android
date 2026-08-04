package com.stripe.android.testing

import org.junit.runner.Runner
import org.junit.runner.manipulation.Filterable
import org.junit.runners.model.RunnerBuilder

/**
 * Custom [RunnerBuilder] registered with [StripeAndroidJUnitRunner] that wraps the default Android
 * test runner with retry support.
 */
internal class RetryRunnerBuilder : RunnerBuilder() {
    private val delegateBuilder = StripeAndroidRunnerBuilder()

    override fun runnerForClass(testClass: Class<*>): Runner? {
        val delegate = delegateBuilder.runnerForClass(testClass) ?: return null

        if (delegate !is Filterable) {
            return delegate
        }

        return if (StripeRetryConfig.shouldRetry()) {
            RetryingRunner(
                testClass = testClass,
                delegateRunner = delegate,
                runnerBuilder = delegateBuilder,
                maxAttempts = StripeRetryConfig.maxAttempts(),
            )
        } else {
            delegate
        }
    }
}
