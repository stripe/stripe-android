package com.stripe.android.testing

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

/**
 * Instrumentation runner that retries failed tests and reports results to JUnit.
 *
 * The initial attempt uses the original test name. Retries are reported as separate JUnit test cases
 * postfixed with `_stripe_retry_NUMBER` (for example `success_stripe_retry_1`).
 *
 * Retry behavior is controlled by instrumentation arguments:
 * - `retryEnabled` (`true`/`false`): whether failed tests are retried.
 * - `retryCount`: maximum number of attempts per test (including the initial run). Used only when
 *   retries are enabled.
 */
open class StripeAndroidJUnitRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        val args = Bundle(arguments ?: Bundle.EMPTY)

        StripeRetryConfig.set(
            retryCount = args.getRetryCount(),
            enabled = BuildConfig.IS_RUNNING_IN_CI,
        )

        if (StripeRetryConfig.shouldRetry()) {
            appendRunnerBuilder(args)
        }

        super.onCreate(args)
    }

    private fun appendRunnerBuilder(arguments: Bundle) {
        val retryRunnerBuilder = RetryRunnerBuilder::class.java.name
        val existingBuilder = arguments.getString(RUNNER_BUILDER_ARGUMENT)
        arguments.putString(
            RUNNER_BUILDER_ARGUMENT,
            if (existingBuilder.isNullOrBlank()) {
                retryRunnerBuilder
            } else {
                "$retryRunnerBuilder,$existingBuilder"
            },
        )
    }

    private fun Bundle.getRetryCount() = getString(ARG_RETRY_COUNT)?.toIntOrNull() ?: DEFAULT_RETRY_COUNT

    private companion object {
        const val RUNNER_BUILDER_ARGUMENT = "runnerBuilder"
        const val ARG_RETRY_COUNT = "retryCount"
        const val DEFAULT_RETRY_COUNT = 1
    }
}
