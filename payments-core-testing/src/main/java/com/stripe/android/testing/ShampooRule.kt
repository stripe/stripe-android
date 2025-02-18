package com.stripe.android.testing

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Got flaky tests? Shampoo them away.  */
class ShampooRule(private val iterations: Int) : TestRule {
    init {
        require(iterations >= 1) { "iterations < 1: $iterations" }
    }

    override fun apply(base: Statement, description: Description?): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                repeat(iterations) { iteration ->
                    try {
                        base.evaluate()
                    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                        println("Failed on iteration: $iteration")
                        throw e
                    }
                }
            }
        }
    }
}
