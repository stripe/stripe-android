package com.stripe.android.utils

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.stripe.android.test.core.DisableAnimationsRule
import com.stripe.android.test.core.INDIVIDUAL_TEST_TIMEOUT_SECONDS
import com.stripe.android.test.core.TestWatcher
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.Timeout
import org.junit.runner.Description
import org.junit.runners.model.Statement

class TestRules private constructor(
    private val chain: RuleChain,
    val compose: ComposeTestRule,
) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val statement = chain.apply(base, description)
                statement.evaluate()
            }
        }
    }

    companion object {

        fun create(
            disableAnimations: Boolean = true,
            retryCount: Int = 3,
        ): TestRules {
            val composeTestRule = createEmptyComposeRule()

            val chain = RuleChain
                .outerRule(Timeout.seconds(INDIVIDUAL_TEST_TIMEOUT_SECONDS))
                .around(composeTestRule)
                .around(TestWatcher())
                .let { chain ->
                    if (disableAnimations) {
                        chain.around(DisableAnimationsRule())
                    } else {
                        chain
                    }
                }
                .around(CleanupChromeRule)
                .around(RetryRule(retryCount))

            return TestRules(chain, composeTestRule)
        }
    }
}
