package com.stripe.android.paymentsheet.utils

import android.app.Application
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.platform.app.InstrumentationRegistry
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
    private val profileLeakChecks: Boolean,
) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val timings = if (profile) TestTimingProfile(System::nanoTime) else null
                val leakChecks = if (profileLeakChecks) LeakCheckProfile(System::nanoTime) else null
                val activityCollection = if (profileLeakChecks) {
                    val application = InstrumentationRegistry.getInstrumentation()
                        .targetContext.applicationContext as Application
                    ActivityCollectionProfile(application, System::nanoTime)
                } else {
                    null
                }
                val execution = timings?.executionStatement(base) ?: base
                val rules = chain.apply(execution, description)
                val measuredRules = timings?.rulesStatement(rules) ?: rules
                val activityRules = activityCollection?.rulesStatement(measuredRules) ?: measuredRules
                val leakStatement = DetectLeaksAfterTestSuccess().apply(
                    leakChecks?.rulesStatement(activityRules) ?: activityRules,
                    description,
                )
                val observedLeaks = leakChecks?.leakStatement(leakStatement) ?: leakStatement
                val statement = activityCollection?.statement(observedLeaks) ?: observedLeaks
                if (timings == null) {
                    statement.evaluate()
                } else {
                    timings.evaluate(statement) { durations, outcome ->
                        writeTestTimingReport(
                            description,
                            durations,
                            outcome,
                            leakChecks?.snapshot(),
                            activityCollection?.snapshot(),
                        )
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
            profileLeakChecks: Boolean = false,
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
            return TestRules(chain, composeTestRule, networkRule, profile, profileLeakChecks)
        }
    }
}
