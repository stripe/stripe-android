package com.stripe.android.paymentsheet.utils

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.stripe.android.networktesting.NetworkRule
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class TestRules private constructor(
    private val chain: RuleChain,
    val compose: ComposeTestRule,
    val networkRule: NetworkRule,
    private val profile: Boolean,
) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val timings = if (profile) TestTimingProfile(System::nanoTime) else null
                val execution = timings?.executionStatement(base) ?: base
                val rules = chain.apply(execution, description)
                val statement = DetectLeaksAfterTestSuccess().apply(
                    timings?.rulesStatement(rules) ?: rules,
                    description,
                )
                if (timings == null) {
                    statement.evaluate()
                } else {
                    timings.evaluate(statement) { durations, outcome ->
                        writeTestTimingReport(description, durations, outcome)
                    }
                }
            }
        }
    }

    companion object {
        fun create(
            composeTestRule: ComposeTestRule = createEmptyComposeRule(),
            networkRule: NetworkRule = NetworkRule(),
            terminalTestRule: TerminalWrapperTestRule = TerminalWrapperTestRule(enabled = false),
            retryRule: TestRule? = null,
            profile: Boolean = false,
            block: RuleChain.() -> RuleChain = { this }
        ): TestRules {
            val chain = RuleChain.emptyRuleChain()
                .around(FakeGooglePayRepositoryRule())
                .around(composeTestRule)
                .around(PrefsTestStoreRule())
                .let { chain ->
                    retryRule?.let(chain::around) ?: chain
                }
                .around(networkRule)
                .around(terminalTestRule)
                .block()
            return TestRules(chain, composeTestRule, networkRule, profile)
        }
    }
}
