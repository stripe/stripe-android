package com.stripe.android.testing

import android.util.Log
import leakcanary.NoLeakAssertionFailedError
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RetryRule(private val attempts: Int) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        val logTag = description.className
        return object : Statement() {
            @Throws
            override fun evaluate() {
                for (attempt in 1..attempts) {
                    val isLast = attempts == attempt
                    runCatching {
                        base.evaluate()
                    }.onSuccess {
                        return
                    }.onFailure { error ->
                        if (isLast || error is AssumptionViolatedException || error is NoLeakAssertionFailedError) {
                            throw error
                        }
                        Log.d(logTag, "Failed attempt $attempt out of $attempts with error")
                    }
                }
            }
        }
    }
}
