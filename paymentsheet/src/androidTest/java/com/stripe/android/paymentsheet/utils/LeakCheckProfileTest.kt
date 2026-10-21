package com.stripe.android.paymentsheet.utils

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runners.model.Statement
import shark.SharkLog

internal class LeakCheckProfileTest {
    @Test
    fun fastGcDecisionRecordsReasonOffsetAndAssertionDuration() = runScenario(initialLogger = FakeSharkLogger()) {
        val logger = requireNotNull(initialLogger)

        checkStatement(rules = statement {
            advance(40L)
            SharkLog.d { NO_RETAINED_OBJECTS_AFTER_DELAY }
            assertThat(profile.snapshot().observerStarted).isFalse()
            assertThat(profile.snapshot().noHeapAnalysisReason).isNull()
        }) {
            advance(7_250_000L)
            SharkLog.d { NO_WATCHED_OBJECTS_AFTER_GC }
            advance(100_000L)
            SharkLog.d { ASSERTION_DURATION }
        }.evaluate()

        val diagnostics = profile.snapshot()
        assertThat(diagnostics.observerStarted).isTrue()
        assertThat(diagnostics.noHeapAnalysisReason).isEqualTo(LeakCheckReason.NoWatchedObjectsAfterGc)
        assertThat(diagnostics.decisionAfterNanos).isEqualTo(7_250_000L)
        assertThat(diagnostics.assertionDurationMillis).isEqualTo(145L)
        assertThat(logger.calls.awaitItem()).isEqualTo(
            LogCall(throwable = null, message = NO_RETAINED_OBJECTS_AFTER_DELAY),
        )
        assertThat(logger.calls.awaitItem()).isEqualTo(LogCall(throwable = null, message = NO_WATCHED_OBJECTS_AFTER_GC))
        assertThat(logger.calls.awaitItem()).isEqualTo(LogCall(throwable = null, message = ASSERTION_DURATION))
    }

    @Test
    fun slowRetentionDelayDecisionRecordsLongFakeClockOffset() = runScenario(initialLogger = FakeSharkLogger()) {
        val logger = requireNotNull(initialLogger)

        checkStatement(rules = statement {}) {
            advance(SLOW_RETENTION_DECISION_NANOS)
            SharkLog.d { NO_RETAINED_OBJECTS_AFTER_DELAY }
        }.evaluate()

        val diagnostics = profile.snapshot()
        assertThat(diagnostics.noHeapAnalysisReason).isEqualTo(LeakCheckReason.NoRetainedObjectsAfterDelay)
        assertThat(diagnostics.decisionAfterNanos).isEqualTo(SLOW_RETENTION_DECISION_NANOS)
        assertThat(diagnostics.assertionDurationMillis).isNull()
        assertThat(logger.calls.awaitItem()).isEqualTo(
            LogCall(throwable = null, message = NO_RETAINED_OBJECTS_AFTER_DELAY),
        )
    }

    @Test
    fun delegatesUnrelatedAndThrowableMessagesWithoutCapturingThem() = runScenario(initialLogger = FakeSharkLogger()) {
        val logger = requireNotNull(initialLogger)
        val throwable = IllegalStateException("unrelated logger failure")

        checkStatement(rules = statement {}) {
            SharkLog.d { UNRELATED_MESSAGE }
            assertThat(profile.snapshot().noHeapAnalysisReason).isNull()

            SharkLog.d(throwable) { NO_WATCHED_OBJECTS_AFTER_GC }
            assertThat(profile.snapshot().noHeapAnalysisReason).isNull()
        }.evaluate()

        val diagnostics = profile.snapshot()
        assertThat(diagnostics.observerStarted).isTrue()
        assertThat(diagnostics.noHeapAnalysisReason).isNull()
        assertThat(diagnostics.decisionAfterNanos).isNull()
        assertThat(diagnostics.assertionDurationMillis).isNull()
        assertThat(logger.calls.awaitItem()).isEqualTo(LogCall(throwable = null, message = UNRELATED_MESSAGE))
        val throwableCall = logger.calls.awaitItem()
        assertThat(throwableCall.throwable).isSameInstanceAs(throwable)
        assertThat(throwableCall.message).isEqualTo(NO_WATCHED_OBJECTS_AFTER_GC)
    }

    @Test
    fun restoresOriginalLoggerAfterSuccessfulOuterCheck() = runScenario(initialLogger = FakeSharkLogger()) {
        val logger = requireNotNull(initialLogger)

        checkStatement(rules = statement {}) {
            advance(23L)
            SharkLog.d { NO_WATCHED_OBJECTS_AFTER_GC }
        }.evaluate()

        assertThat(SharkLog.logger).isSameInstanceAs(logger)
        assertThat(profile.snapshot().noHeapAnalysisReason).isEqualTo(LeakCheckReason.NoWatchedObjectsAfterGc)
        assertThat(logger.calls.awaitItem()).isEqualTo(LogCall(throwable = null, message = NO_WATCHED_OBJECTS_AFTER_GC))
    }

    @Test
    fun restoresOriginalLoggerAfterFailingOuterCheckAndPreservesFailure() =
        runScenario(initialLogger = FakeSharkLogger()) {
        val logger = requireNotNull(initialLogger)
        val failure = AssertionError("outer leak check failed")

        val thrown = assertThrows(AssertionError::class.java) {
            checkStatement(rules = statement {}) {
                SharkLog.d { NO_WATCHED_OBJECTS_AFTER_GC }
                throw failure
            }.evaluate()
        }

        assertThat(thrown).isSameInstanceAs(failure)
        assertThat(SharkLog.logger).isSameInstanceAs(logger)
        assertThat(profile.snapshot().noHeapAnalysisReason).isEqualTo(LeakCheckReason.NoWatchedObjectsAfterGc)
        assertThat(logger.calls.awaitItem()).isEqualTo(LogCall(throwable = null, message = NO_WATCHED_OBJECTS_AFTER_GC))
    }

    @Test
    fun sharedRulesFailureDoesNotInstallObserver() = runScenario(initialLogger = FakeSharkLogger()) {
        val logger = requireNotNull(initialLogger)
        val failure = IllegalStateException("shared rules failed")

        val thrown = assertThrows(IllegalStateException::class.java) {
            checkStatement(rules = statement { throw failure }) {
                error("the outer check should not run")
            }.evaluate()
        }

        assertThat(thrown).isSameInstanceAs(failure)
        assertThat(profile.snapshot().observerStarted).isFalse()
        assertThat(SharkLog.logger).isSameInstanceAs(logger)
        logger.calls.expectNoEvents()
    }

    @Test
    fun nullOriginalLoggerKeepsDebugLambdaDisabled() = runScenario(initialLogger = null) {
        checkStatement(rules = statement {}) {
            SharkLog.d {
                debugMessageEvaluations.add(Unit)
                "debug message"
            }
        }.evaluate()

        assertThat(profile.snapshot().observerStarted).isFalse()
        assertThat(SharkLog.logger).isNull()
        debugMessageEvaluations.expectNoEvents()
    }

    @Test
    fun otherThreadLogsAreDelegatedButNotCaptured() = runScenario(initialLogger = FakeSharkLogger()) {
        val logger = requireNotNull(initialLogger)

        checkStatement(rules = statement {}) {
            val thread = Thread {
                try {
                    SharkLog.d { NO_WATCHED_OBJECTS_AFTER_GC }
                } catch (error: Throwable) {
                    threadFailures.add(error)
                }
            }
            thread.start()
            thread.join(5_000L)
            assertThat(thread.isAlive).isFalse()
            threadFailures.expectNoEvents()

            assertThat(profile.snapshot().noHeapAnalysisReason).isNull()
            advance(43L)
            SharkLog.d { NO_WATCHED_OBJECTS_AFTER_GC }
        }.evaluate()

        assertThat(profile.snapshot().noHeapAnalysisReason).isEqualTo(LeakCheckReason.NoWatchedObjectsAfterGc)
        assertThat(profile.snapshot().decisionAfterNanos).isEqualTo(43L)
        assertThat(logger.calls.awaitItem()).isEqualTo(LogCall(throwable = null, message = NO_WATCHED_OBJECTS_AFTER_GC))
        assertThat(logger.calls.awaitItem()).isEqualTo(LogCall(throwable = null, message = NO_WATCHED_OBJECTS_AFTER_GC))
    }

    @Test
    fun keepsReplacementLoggerInstalledDuringCheck() = runScenario(initialLogger = FakeSharkLogger()) {
        val logger = requireNotNull(initialLogger)
        val replacementLogger = this.replacementLogger

        checkStatement(rules = statement {}) {
            advance(17L)
            SharkLog.d { NO_WATCHED_OBJECTS_AFTER_GC }
            SharkLog.logger = replacementLogger
            SharkLog.d { MESSAGE_AFTER_REPLACEMENT }
        }.evaluate()

        assertThat(SharkLog.logger).isSameInstanceAs(replacementLogger)
        assertThat(profile.snapshot().noHeapAnalysisReason).isEqualTo(LeakCheckReason.NoWatchedObjectsAfterGc)
        assertThat(logger.calls.awaitItem()).isEqualTo(LogCall(throwable = null, message = NO_WATCHED_OBJECTS_AFTER_GC))
        assertThat(replacementLogger.calls.awaitItem()).isEqualTo(
            LogCall(throwable = null, message = MESSAGE_AFTER_REPLACEMENT),
        )
    }

    private fun runScenario(
        initialLogger: FakeSharkLogger?,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val previousLogger = SharkLog.logger
        val scenario = Scenario(initialLogger = initialLogger)
        SharkLog.logger = initialLogger
        try {
            scenario.block()
            scenario.ensureAllEventsConsumed()
        } finally {
            SharkLog.logger = previousLogger
        }
    }

    private class Scenario(
        val initialLogger: FakeSharkLogger?,
    ) {
        var nowNanos = 0L
            private set
        val profile = LeakCheckProfile { nowNanos }
        val replacementLogger = FakeSharkLogger()
        val debugMessageEvaluations = Turbine<Unit>()
        val threadFailures = Turbine<Throwable>()

        fun advance(nanos: Long) {
            nowNanos += nanos
        }

        fun checkStatement(
            rules: Statement,
            check: () -> Unit,
        ): Statement {
            val profiledRules = profile.rulesStatement(rules)
            return profile.leakStatement(statement {
                profiledRules.evaluate()
                check()
            })
        }

        fun statement(block: () -> Unit): Statement = object : Statement() {
            override fun evaluate() = block()
        }

        fun ensureAllEventsConsumed() {
            initialLogger?.ensureAllEventsConsumed()
            replacementLogger.ensureAllEventsConsumed()
            debugMessageEvaluations.ensureAllEventsConsumed()
            threadFailures.ensureAllEventsConsumed()
        }
    }

    private class FakeSharkLogger : SharkLog.Logger {
        val calls = Turbine<LogCall>()

        override fun d(message: String) {
            calls.add(LogCall(throwable = null, message = message))
        }

        override fun d(throwable: Throwable, message: String) {
            calls.add(LogCall(throwable = throwable, message = message))
        }

        fun ensureAllEventsConsumed() {
            calls.ensureAllEventsConsumed()
        }
    }

    private data class LogCall(
        val throwable: Throwable?,
        val message: String,
    )

    private companion object {
        const val NO_WATCHED_OBJECTS_AFTER_GC =
            "Test can keep going: no heap dump performed (No watched objects after triggering an explicit GC.)"
        const val NO_RETAINED_OBJECTS_AFTER_DELAY =
            "Test can keep going: no heap dump performed (No retained objects after waiting for retained delay.)"
        const val ASSERTION_DURATION =
            "Spent 145 ms detecting leaks on DetectLeaksAfterTestSuccess, VM total so far: 812 ms"
        const val UNRELATED_MESSAGE = "unrelated watched object details"
        const val MESSAGE_AFTER_REPLACEMENT = "log after replacement"
        const val SLOW_RETENTION_DECISION_NANOS = 5_200_000_000L
    }
}
