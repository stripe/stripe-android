package com.stripe.android.paymentsheet.utils

import android.app.Activity
import android.provider.Settings
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
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
    leakCheck: LeakCheckDiagnostics?,
    activityCollection: ActivityCollectionDiagnostics?,
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
        if (leakCheck != null) {
            put("leak_check", JSONObject().apply {
                put("observer_started", leakCheck.observerStarted)
                put("no_heap_analysis_reason", leakCheck.noHeapAnalysisReason?.name ?: JSONObject.NULL)
                put("decision_after_ns", leakCheck.decisionAfterNanos ?: JSONObject.NULL)
                put("assertion_duration_ms", leakCheck.assertionDurationMillis ?: JSONObject.NULL)
            })
        }
        if (activityCollection != null) {
            put("activity_collection", activityCollection.toJson())
            put("animation_scales", readAnimationScales())
        }
    }.toString()
    Log.i(TEST_TIMING_TAG, report)
    val outputDirectory = arguments.getString("additionalTestOutputDir") ?: return
    val directory = File(outputDirectory, "paymentsheet-test-timings")
    check(directory.isDirectory || directory.mkdirs()) { "Cannot create test timing output directory" }
    File(directory, "${UUID.randomUUID()}.json").writeText(report)
}

private fun ActivityCollectionDiagnostics.toJson() = JSONObject().apply {
    put("observer_started", observerStarted)
    put("observer_completed", observerCompleted)
    put("worker_exited_unexpectedly", workerExitedUnexpectedly)
    put("leak_check_started_after_ns", leakCheckStartedAfterNanos ?: JSONObject.NULL)
    put("leak_check_finished_after_ns", leakCheckFinishedAfterNanos ?: JSONObject.NULL)
    put("finished_after_ns", finishedAfterNanos ?: JSONObject.NULL)
    put("activities", JSONArray(activities.map { it.toJson() }))
}

private fun ActivityCollectionObservation.toJson() = JSONObject().apply {
    put("type", type.name)
    put("destroyed_after_ns", destroyedAfterNanos)
    put("collection_observed_after_ns", collectionObservedAfterNanos ?: JSONObject.NULL)
}

private fun readAnimationScales(): JSONObject {
    val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
    return JSONObject().apply {
        put(
            "window",
            Settings.Global.getString(resolver, Settings.Global.WINDOW_ANIMATION_SCALE) ?: JSONObject.NULL,
        )
        put(
            "transition",
            Settings.Global.getString(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE) ?: JSONObject.NULL,
        )
        put(
            "animator",
            Settings.Global.getString(resolver, Settings.Global.ANIMATOR_DURATION_SCALE) ?: JSONObject.NULL,
        )
    }
}
