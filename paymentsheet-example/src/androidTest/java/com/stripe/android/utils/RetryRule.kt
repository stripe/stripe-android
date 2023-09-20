package com.stripe.android.utils

import android.util.Log
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
                    val error = runCatching { base.evaluate() }.exceptionOrNull()

                    if (error != null) {
                        if (isLast || error is AssumptionViolatedException) {
                            throw error
                        }
                        Log.d(logTag, "Failed attempt $attempt out of $attempts with error")
                    } else {
                        // The test succeeded
                        return
                    }
                }
            }
        }
    }
}
