package com.stripe.android.paymentsheet.utils

import android.app.Activity
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.AssumptionViolatedException
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.util.UUID

private const val TEST_TIMING_TAG = "PaymentSheetTestTiming"

internal enum class TestTimingPhase {
    RuleSetup,
    ActivitySetup,
    ScenarioBody,
    Completion,
    ActivityCleanup,
    JunitExecutionOther,
    RuleCleanup,
    LeakRule,
}

internal class TestTimingProfile(private val nanoTime: () -> Long) {
    private val durations = TestTimingPhase.entries.associateWith { 0L }.toMutableMap()
    private var phase = TestTimingPhase.LeakRule
    private var startedAt = 0L

    fun evaluate(statement: Statement, report: (Map<TestTimingPhase, Long>, String) -> Unit) {
        val previous = current.get()
        current.set(this)
        startedAt = nanoTime()
        var failure: Throwable? = null
        try {
            statement.evaluate()
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            switchTo(phase)
            current.set(previous)
            val outcome = when (failure) {
                null -> "passed"
                is AssumptionViolatedException -> "skipped"
                else -> "failed"
            }
            try {
                report(durations.toMap(), outcome)
            } catch (reportError: Throwable) {
                Log.w(TEST_TIMING_TAG, "Could not export test timing report", reportError)
                failure?.addSuppressed(reportError)
            }
        }
    }

    fun rulesStatement(base: Statement): Statement = object : Statement() {
        override fun evaluate() = measure(TestTimingPhase.RuleSetup) { base.evaluate() }
    }

    fun executionStatement(base: Statement): Statement = object : Statement() {
        override fun evaluate() {
            try {
                measure(TestTimingPhase.JunitExecutionOther) { base.evaluate() }
            } finally {
                switchTo(TestTimingPhase.RuleCleanup)
            }
        }
    }

    private fun <T> measure(nextPhase: TestTimingPhase, block: () -> T): T {
        val previous = phase
        switchTo(nextPhase)
        try {
            return block()
        } finally {
            switchTo(previous)
        }
    }

    private fun switchTo(nextPhase: TestTimingPhase) {
        val now = nanoTime()
        durations[phase] = durations.getValue(phase) + now - startedAt
        phase = nextPhase
        startedAt = now
    }

    companion object {
        private val current = ThreadLocal<TestTimingProfile>()

        fun <T> measure(phase: TestTimingPhase, block: () -> T): T {
            val profile = current.get()
            return if (profile == null) block() else profile.measure(phase, block)
        }
    }
}

internal fun <T : Activity> withProfiledActivityScenario(
    activityClass: Class<T>,
    block: (ActivityScenario<T>) -> Unit,
) {
    val scenario = TestTimingProfile.measure(TestTimingPhase.ActivitySetup) {
        ActivityScenario.launch(activityClass)
    }
    TestTimingProfile.measure(TestTimingPhase.ActivityCleanup) {
        scenario.use {
            TestTimingProfile.measure(TestTimingPhase.JunitExecutionOther) { block(it) }
        }
    }
}

internal fun writeTestTimingReport(
    description: Description,
    durations: Map<TestTimingPhase, Long>,
    outcome: String,
) {
    val arguments = InstrumentationRegistry.getArguments()
    val report = JSONObject().apply {
        put("class", description.className)
        put("test", description.displayName)
        put("method", description.methodName)
        put("shard_index", arguments.getString("shardIndex"))
        put("outcome", outcome)
        put("total_ns", durations.values.sum())
        put("phases_ns", JSONObject(durations.mapKeys { it.key.name }))
    }.toString()
    Log.i(TEST_TIMING_TAG, report)
    val outputDirectory = arguments.getString("additionalTestOutputDir") ?: return
    val directory = File(outputDirectory, "paymentsheet-test-timings")
    check(directory.isDirectory || directory.mkdirs()) { "Cannot create test timing output directory" }
    File(directory, "${UUID.randomUUID()}.json").writeText(report)
}
