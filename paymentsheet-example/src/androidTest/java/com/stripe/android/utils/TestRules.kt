package com.stripe.android.utils

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.stripe.android.paymentsheet.example.BuildConfig
import com.stripe.android.testing.RetryRule
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
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

            val chain = RuleChain.emptyRuleChain()
                .around(DetectLeaksAfterTestSuccess())
                .around(composeTestRule)
                .let { chain ->
                    if (disableAnimations) {
                        chain.around(DisableAnimationsRule())
                    } else {
                        chain
                    }
                }.let { chain ->
                    if (BuildConfig.IS_RUNNING_IN_CI) {
                        chain.around(RetryRule(retryCount))
                    } else {
                        chain
                    }
                }
                .around(CleanupChromeRule)

            return TestRules(chain, composeTestRule)
        }
    }
}
