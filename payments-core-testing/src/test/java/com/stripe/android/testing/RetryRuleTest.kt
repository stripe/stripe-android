package com.stripe.android.testing

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.junit.runners.model.TestTimedOutException
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class RetryRuleTest {
    @Test
    fun `timed out tests are not retried`() = runScenario(
        failure = TestTimedOutException(90, TimeUnit.SECONDS),
    ) {
        assertFailure()

        statement.evaluateCalls.awaitItem()
    }

    @Test
    fun `ordinary failures are retried in CI`() = runScenario {
        assertFailure()

        repeat(3) {
            statement.evaluateCalls.awaitItem()
        }
    }

    @Test
    fun `assumption violations are not retried`() = runScenario(
        failure = AssumptionViolatedException("Test not applicable"),
    ) {
        assertFailure()

        statement.evaluateCalls.awaitItem()
    }

    @Test
    fun `failures outside CI are not retried`() = runScenario(isRunningInCi = false) {
        assertFailure()

        statement.evaluateCalls.awaitItem()
    }

    @Test
    fun `successful tests are not retried`() = runScenario(failure = null) {
        rule.evaluate()

        statement.evaluateCalls.awaitItem()
    }

    private fun runScenario(
        failure: Throwable? = AssertionError("Test failed"),
        isRunningInCi: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val statement = FakeStatement(failure)
        val rule = RetryRule(attempts = 3, isRunningInCi = isRunningInCi).apply(
            statement,
            Description.createTestDescription(RetryRuleTest::class.java, "test"),
        )

        Scenario(rule, statement, failure).block()

        statement.evaluateCalls.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val rule: Statement,
        val statement: FakeStatement,
        val failure: Throwable?,
    ) {
        fun assertFailure() {
            val thrown = assertThrows(Throwable::class.java) { rule.evaluate() }
            assertThat(thrown).isSameInstanceAs(failure)
        }
    }

    private class FakeStatement(private val failure: Throwable?) : Statement() {
        val evaluateCalls = Turbine<Unit>()

        override fun evaluate() {
            evaluateCalls.add(Unit)
            failure?.let { throw it }
        }
    }
}
