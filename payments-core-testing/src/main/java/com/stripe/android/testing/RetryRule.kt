package com.stripe.android.testing

import android.util.Log
import leakcanary.NoLeakAssertionFailedError
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.junit.runners.model.TestTimedOutException

class RetryRule internal constructor(
    private val attempts: Int,
    private val isRunningInCi: Boolean,
) : TestRule {
    constructor(attempts: Int) : this(attempts, BuildConfig.IS_RUNNING_IN_CI)

    override fun apply(base: Statement, description: Description): Statement {
        val logTag = description.className
        return object : Statement() {
            @Throws
            override fun evaluate() {
                if (isRunningInCi) {
                    for (attempt in 1..attempts) {
                        val isLast = attempts == attempt
                        runCatching {
                            base.evaluate()
                        }.onSuccess {
                            return
                        }.onFailure { error ->
                            // A timed-out worker may still have an Espresso wait running on the main thread.
                            // Starting another attempt can re-enter Espresso before that wait has finished.
                            if (
                                isLast || error is AssumptionViolatedException ||
                                error is NoLeakAssertionFailedError || error is TestTimedOutException
                            ) {
                                throw error
                            }
                            Log.d(logTag, "Failed attempt $attempt out of $attempts with error")
                        }
                    }
                } else {
                    base.evaluate()
                }
            }
        }
    }
}
