@file:Suppress("unused")

package com.stripe.android.testing

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RetryRule(private val attempts: Int) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws
            override fun evaluate() {
                base.evaluate()
            }
        }
    }
}
