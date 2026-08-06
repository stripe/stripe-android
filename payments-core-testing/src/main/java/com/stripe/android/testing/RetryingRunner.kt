package com.stripe.android.testing

import android.util.Log
import leakcanary.NoLeakAssertionFailedError
import org.junit.AssumptionViolatedException
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.manipulation.Filter
import org.junit.runner.manipulation.Filterable
import org.junit.runner.manipulation.NoTestsRemainException
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier

internal class RetryingRunner(
    private val testClass: Class<*>,
    private val delegateRunner: Runner,
    private val runnerBuilder: StripeAndroidRunnerBuilder,
    private val maxAttempts: Int,
) : Runner(), Filterable {
    override fun getDescription(): Description = delegateRunner.description

    override fun filter(filter: Filter) {
        val filterable = delegateRunner as? Filterable ?: throw NoTestsRemainException()
        filterable.filter(filter)
    }

    override fun run(notifier: RunNotifier) {
        if (maxAttempts <= 1) {
            delegateRunner.run(notifier)
            return
        }

        RetryEngine(
            testClass = testClass,
            runnerBuilder = runnerBuilder,
            maxAttempts = maxAttempts,
            outerNotifier = notifier,
        ).run(delegateRunner)
    }
}

internal class RetryEngine(
    private val testClass: Class<*>,
    private val runnerBuilder: StripeAndroidRunnerBuilder,
    private val maxAttempts: Int,
    private val outerNotifier: RunNotifier,
) {
    fun run(initialRunner: Runner) {
        var pendingDescriptions: List<Description>? = null

        for (attempt in 1..maxAttempts) {
            val isLastAttempt = attempt == maxAttempts
            val attemptFailures = mutableListOf<Failure>()
            val listener = RetryAttemptListener(
                attempt = attempt,
                isLastAttempt = isLastAttempt,
                attemptFailures = attemptFailures,
            )
            val notifier = RunNotifier()
            notifier.addListener(listener)

            if (pendingDescriptions == null) {
                initialRunner.run(notifier)
            } else {
                pendingDescriptions.forEach { description ->
                    runSingleTest(description, notifier)
                }
            }

            val retryableFailures = attemptFailures.filter(::shouldRetry)
            if (retryableFailures.isEmpty()) {
                return
            }

            if (isLastAttempt) {
                return
            }

            pendingDescriptions = retryableFailures.map { it.description }
            Log.i(
                LOG_TAG,
                "Retrying ${pendingDescriptions.size} failed test(s) " +
                    "(attempt ${attempt + 1}/$maxAttempts): " +
                    pendingDescriptions.joinToString { it.displayName },
            )
        }
    }

    private fun runSingleTest(description: Description, notifier: RunNotifier) {
        try {
            val runner = runnerBuilder.runnerForClass(testClass)
                ?: run {
                    Log.w(LOG_TAG, "Could not create runner for ${testClass.name}, skipping retry")
                    return
                }
            val filterable = runner as? Filterable
                ?: run {
                    Log.w(
                        LOG_TAG,
                        "Could not filter runner for ${testClass.name}, skipping retry for " +
                            description.displayName,
                    )
                    return
                }
            val filter = Filter.matchMethodDescription(description)
            filterable.filter(filter)
            runner.run(notifier)
        } catch (exception: NoTestsRemainException) {
            Log.w(LOG_TAG, "Could not retry ${description.displayName}", exception)
        }
    }

    private fun shouldRetry(failure: Failure): Boolean {
        if (failure.description.methodName == "initializationError") {
            return false
        }

        val exception = failure.exception
        return exception !is AssumptionViolatedException && exception !is NoLeakAssertionFailedError
    }

    /**
     * Failed attempts are reported under `_stripe_retry_{attempt}` so the original test name stays
     * available for the final pass/fail result. The initial attempt uses the original name only when
     * it is the final failure.
     */
    private fun failureReportDescription(description: Description, attempt: Int, isFinal: Boolean): Description {
        if (isFinal && attempt == 1) {
            return description
        }
        return Description.createTestDescription(
            description.className,
            "${description.methodName}${STRIPE_RETRY_INFIX}$attempt",
        )
    }

    private fun reportAttemptFailure(failure: Failure, attempt: Int, isFinal: Boolean) {
        val reportDescription = failureReportDescription(failure.description, attempt, isFinal)
        val instrumentationFailure = Failure(reportDescription, failure.exception)
        outerNotifier.fireTestStarted(reportDescription)
        when {
            !isFinal -> {
                // Log flaky intermediate attempts in JUnit output without failing the overall suite
                // when a later retry succeeds.
                outerNotifier.fireTestAssumptionFailed(instrumentationFailure)
            }
            failure.exception is AssumptionViolatedException -> {
                outerNotifier.fireTestAssumptionFailed(instrumentationFailure)
            }
            else -> {
                outerNotifier.fireTestFailure(instrumentationFailure)
            }
        }
        outerNotifier.fireTestFinished(reportDescription)
    }

    private fun reportSuccess(description: Description, attempt: Int) {
        if (attempt > 1) {
            Log.i(
                LOG_TAG,
                "test passed on attempt $attempt/$maxAttempts: ${description.displayName}",
            )
        } else {
            Log.i(LOG_TAG, "test passed: ${description.displayName}")
        }
        outerNotifier.fireTestStarted(description)
        outerNotifier.fireTestFinished(description)
    }

    private inner class RetryAttemptListener(
        private val attempt: Int,
        private val isLastAttempt: Boolean,
        private val attemptFailures: MutableList<Failure>,
    ) : RunListener() {
        override fun testFailure(failure: Failure) {
            attemptFailures.add(failure)
            val isFinal = !shouldRetry(failure) || isLastAttempt
            reportAttemptFailure(failure, attempt, isFinal)

            if (isFinal) {
                Log.e(
                    LOG_TAG,
                    "test failed after $attempt attempt(s): ${failure.description.displayName}",
                    failure.exception,
                )
            } else {
                Log.w(
                    LOG_TAG,
                    "test failed on attempt $attempt/$maxAttempts (will retry): " +
                        failure.description.displayName,
                    failure.exception,
                )
            }
        }

        override fun testAssumptionFailure(failure: Failure) {
            attemptFailures.add(failure)
            reportAttemptFailure(failure, attempt, isFinal = true)
            Log.e(
                LOG_TAG,
                "test assumption failed after $attempt attempt(s): ${failure.description.displayName}",
                failure.exception,
            )
        }

        override fun testFinished(description: Description) {
            val failedThisAttempt = attemptFailures.any { it.description == description }
            if (failedThisAttempt) {
                return
            }

            reportSuccess(description, attempt)
        }
    }

    private companion object {
        const val LOG_TAG = "StripeAndroidJUnitRunner"
        const val STRIPE_RETRY_INFIX = "_stripe_retry_"
    }
}
