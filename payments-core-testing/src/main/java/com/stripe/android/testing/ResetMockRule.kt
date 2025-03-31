package com.stripe.android.testing

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.mockito.kotlin.reset

class ResetMockRule(vararg mocks: Any) : TestRule {
    private val mocksArray: Array<out Any> = mocks

    override fun apply(
        base: Statement,
        description: Description
    ): Statement {
        return object : Statement() {
            @Throws
            override fun evaluate() {
                base.evaluate()
                @Suppress("SpreadOperator") // Spread operator is required for desired behavior.
                reset(*mocksArray)
            }
        }
    }
}
