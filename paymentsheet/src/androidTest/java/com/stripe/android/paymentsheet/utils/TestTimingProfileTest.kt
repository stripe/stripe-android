package com.stripe.android.paymentsheet.utils

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runners.model.Statement

internal class TestTimingProfileTest {
    @Test
    fun nestedPhasesAreExclusiveAndRepeatedSessionsAreSummed() = runScenario {
        val execution = profile.executionStatement(statement {
            advance(7)
            repeat(2) {
                TestTimingProfile.measure(TestTimingPhase.ActivitySetup) { advance(11) }
                TestTimingProfile.measure(TestTimingPhase.ScenarioBody) { advance(13) }
                TestTimingProfile.measure(TestTimingPhase.Completion) { advance(17) }
                TestTimingProfile.measure(TestTimingPhase.ActivityCleanup) { advance(19) }
            }
            advance(23) // JUnit lifecycle work also belongs to the execution interval.
        })
        val rules = profile.rulesStatement(statement {
            advance(29)
            execution.evaluate()
            advance(31)
        })
        evaluate(statement {
            rules.evaluate()
            advance(37)
        })

        val report = reports.awaitItem()
        assertThat(report.outcome).isEqualTo("passed")
        assertThat(report.durations).containsExactly(
            TestTimingPhase.RuleSetup, 29L,
            TestTimingPhase.ActivitySetup, 22L,
            TestTimingPhase.ScenarioBody, 26L,
            TestTimingPhase.Completion, 34L,
            TestTimingPhase.ActivityCleanup, 38L,
            TestTimingPhase.JunitExecutionOther, 30L,
            TestTimingPhase.RuleCleanup, 31L,
            TestTimingPhase.LeakRule, 37L,
        )
        assertThat(report.durations.values.sum()).isEqualTo(elapsedNanos)
    }

    @Test
    fun nullableResultsDoNotRepeatTheMeasuredBlock() = runScenario {
        val calls = Turbine<Unit>()
        evaluate(profile.executionStatement(statement {
            val result: Any? = TestTimingProfile.measure(TestTimingPhase.ScenarioBody) {
                calls.add(Unit)
                advance(11)
                null
            }
            assertThat(result).isNull()
        }))

        calls.awaitItem()
        calls.ensureAllEventsConsumed()
        val report = reports.awaitItem()
        assertThat(report.durations.getValue(TestTimingPhase.ScenarioBody)).isEqualTo(11L)
    }

    @Test
    fun testFailureStillMeasuresCleanupAndPreservesTheFailure() = runScenario {
        val failure = IllegalStateException("test failed")
        val execution = profile.executionStatement(statement {
            try {
                TestTimingProfile.measure(TestTimingPhase.ScenarioBody) {
                    advance(11)
                    throw failure
                }
            } finally {
                advance(13)
            }
        })
        val rules = profile.rulesStatement(statement {
            advance(17)
            try {
                execution.evaluate()
            } finally {
                advance(19)
            }
        })

        val thrown = assertThrows(IllegalStateException::class.java) {
            evaluate(statement {
                rules.evaluate()
                advance(23)
            })
        }

        assertThat(thrown).isSameInstanceAs(failure)
        val report = reports.awaitItem()
        assertThat(report.outcome).isEqualTo("failed")
        assertThat(report.durations.getValue(TestTimingPhase.ScenarioBody)).isEqualTo(11L)
        assertThat(report.durations.getValue(TestTimingPhase.JunitExecutionOther)).isEqualTo(13L)
        assertThat(report.durations.getValue(TestTimingPhase.RuleCleanup)).isEqualTo(19L)
        assertThat(report.durations.getValue(TestTimingPhase.LeakRule)).isEqualTo(0L)
        assertThat(report.durations.values.sum()).isEqualTo(elapsedNanos)
    }

    @Test
    fun setupFailureIncludesUnwindingInTheSetupBucket() = runScenario {
        val failure = IllegalStateException("setup failed")
        val rules = profile.rulesStatement(statement {
            try {
                advance(11)
                throw failure
            } finally {
                advance(13)
            }
        })

        assertThat(assertThrows(IllegalStateException::class.java) { evaluate(rules) }).isSameInstanceAs(failure)
        val report = reports.awaitItem()
        assertThat(report.outcome).isEqualTo("failed")
        assertThat(report.durations.getValue(TestTimingPhase.RuleSetup)).isEqualTo(24L)
        assertThat(report.durations.getValue(TestTimingPhase.RuleCleanup)).isEqualTo(0L)
    }

    @Test
    fun leakFailureIsReportedWithoutChangingTheFailure() = runScenario {
        val failure = AssertionError("leak detected")
        val execution = profile.executionStatement(statement { advance(11) })
        val rules = profile.rulesStatement(execution)

        val thrown = assertThrows(AssertionError::class.java) {
            evaluate(statement {
                rules.evaluate()
                advance(13)
                throw failure
            })
        }

        assertThat(thrown).isSameInstanceAs(failure)
        val report = reports.awaitItem()
        assertThat(report.outcome).isEqualTo("failed")
        assertThat(report.durations.getValue(TestTimingPhase.LeakRule)).isEqualTo(13L)
    }

    @Test
    fun reportFailurePreservesASuccessfulTest() = runScenario {
        profile.evaluate(statement { advance(11) }) { _, _ -> error("report failed") }

        assertThat(elapsedNanos).isEqualTo(11L)
    }

    @Test
    fun reportFailureDoesNotMaskTheTestFailure() = runScenario {
        val testFailure = IllegalStateException("test failed")
        val reportFailure = IllegalArgumentException("report failed")

        val thrown = assertThrows(IllegalStateException::class.java) {
            profile.evaluate(statement { throw testFailure }) { _, _ -> throw reportFailure }
        }

        assertThat(thrown).isSameInstanceAs(testFailure)
        assertThat(thrown.suppressed).asList().containsExactly(reportFailure)
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val scenario = Scenario()
        scenario.block()
        scenario.reports.ensureAllEventsConsumed()
    }

    private class Scenario {
        var elapsedNanos = 0L
            private set
        val profile = TestTimingProfile { elapsedNanos }
        val reports = Turbine<Report>()

        fun advance(nanos: Long) {
            elapsedNanos += nanos
        }

        fun evaluate(statement: Statement) {
            profile.evaluate(statement) { durations, outcome -> reports.add(Report(durations, outcome)) }
        }

        fun statement(block: () -> Unit): Statement = object : Statement() {
            override fun evaluate() = block()
        }
    }

    private data class Report(val durations: Map<TestTimingPhase, Long>, val outcome: String)
}
