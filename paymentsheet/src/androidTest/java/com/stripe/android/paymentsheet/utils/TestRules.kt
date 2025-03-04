package com.stripe.android.paymentsheet.utils

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.testing.RetryRule
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class TestRules private constructor(
    private val chain: RuleChain,
    val compose: AndroidComposeTestRule<*, *>,
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
            composeTestRule: AndroidComposeTestRule<*, *> = createAndroidComposeRule<PaymentSheetActivity>(),
            networkRule: NetworkRule = NetworkRule(),
        ): TestRules {
            val chain = RuleChain.emptyRuleChain()
                .around(DetectLeaksAfterTestSuccess())
                .around(composeTestRule)
                .around(RetryRule(5))
                .around(networkRule)

            return TestRules(chain, composeTestRule, networkRule)
        }
    }
}
