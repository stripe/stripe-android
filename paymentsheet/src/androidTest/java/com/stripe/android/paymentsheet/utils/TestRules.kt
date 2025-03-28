package com.stripe.android.paymentsheet.utils

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.testing.RetryRule
import com.stripe.android.testing.ShampooRule
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class TestRules private constructor(
    private val chain: RuleChain,
    val compose: ComposeTestRule,
    val networkRule: NetworkRule,
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
            composeTestRule: ComposeTestRule = createEmptyComposeRule(),
            networkRule: NetworkRule = NetworkRule(),
            block: RuleChain.() -> RuleChain = { this }
        ): TestRules {
            val chain =
                RuleChain.emptyRuleChain()
                    .around(DetectLeaksAfterTestSuccess())
                    .around(composeTestRule)
                    .around(RetryRule(5))
                    .around(networkRule)
                    .block()

            return TestRules(chain, composeTestRule, networkRule)
        }
    }
}
