package com.stripe.android.paymentsheet.utils

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import com.stripe.android.networktesting.NetworkRule
import leakcanary.DetectLeaksAfterTestSuccess
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        @OptIn(ExperimentalCoroutinesApi::class)
        fun create(
            composeTestRule: ComposeTestRule = createEmptyComposeRule(
                effectContext = UnconfinedTestDispatcher(),
            ),
            networkRule: NetworkRule = NetworkRule(),
            terminalTestRule: TerminalWrapperTestRule = TerminalWrapperTestRule(enabled = false),
            retryRule: TestRule? = null,
            block: RuleChain.() -> RuleChain = { this }
        ): TestRules {
            val chain = RuleChain.emptyRuleChain()
                .around(DetectLeaksAfterTestSuccess())
                .around(FakeGooglePayRepositoryRule())
                .around(composeTestRule)
                .let { chain ->
                    retryRule?.let(chain::around) ?: chain
                }
                .around(networkRule)
                .around(terminalTestRule)
                .block()
            return TestRules(chain, composeTestRule, networkRule)
        }
    }
}
