package com.stripe.android.stripecardscan.framework.time

import android.util.Log
import androidx.annotation.CheckResult
import androidx.annotation.RestrictTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Measure the amount of time a process takes.
 *
 * TODO: use contracts when they are no longer experimental
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <T> measureTime(block: () -> T): Pair<Duration, T> {
    // contract { callsInPlace(block, EXACTLY_ONCE) }
    val mark = TimeSource.Monotonic.markNow()
    val result = block()
    return mark.elapsedNow() to result
}

internal sealed class Timer {

    companion object {
        fun newInstance(
            tag: String,
            name: String,
            enabled: Boolean,
            updateInterval: Duration = 2.seconds
        ) = if (enabled) {
            LoggingTimer(
                tag,
                name,
                updateInterval
            )
        } else {
            NoOpTimer
        }
    }

    abstract suspend fun <T> measureSuspend(taskName: String? = null, task: suspend () -> T): T
}

private object NoOpTimer : Timer() {

    // TODO: use contracts when they are no longer experimental
    override suspend fun <T> measureSuspend(taskName: String?, task: suspend () -> T): T {
        // contract { callsInPlace(task, EXACTLY_ONCE) }
        return task()
    }
}

private class LoggingTimer(
    private val tag: String,
    private val name: String,
    private val updateInterval: Duration
) : Timer() {
    private var executionCount = 0
    private var executionTotalDuration = Duration.ZERO
    private var updateClock = TimeSource.Monotonic.markNow()

    // TODO: use contracts when they are no longer experimental
    override suspend fun <T> measureSuspend(taskName: String?, task: suspend () -> T): T {
        // contract { callsInPlace(task, EXACTLY_ONCE) }
        val (duration, result) = measureTime { task() }

        executionCount++
        executionTotalDuration += duration

        if (updateClock.elapsedNow() > updateInterval) {
            updateClock = TimeSource.Monotonic.markNow()
            Log.d(
                tag,
                "$name${if (!taskName.isNullOrEmpty()) ".$taskName" else ""} executing on " +
                    "thread ${Thread.currentThread().name} " +
                    "AT ${executionCount.toDouble() / executionTotalDuration.inWholeSeconds} FPS, " +
                    "${executionTotalDuration.inWholeMilliseconds.toDouble() / executionCount} MS/F"
            )
        }
        return result
    }
}
