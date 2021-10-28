package com.stripe.android.cardverificationsheet.framework.time

import android.util.Log
import com.stripe.android.cardverificationsheet.framework.Config
import kotlinx.coroutines.runBlocking

internal sealed class Timer {

    companion object {
        fun newInstance(
            tag: String,
            name: String,
            updateInterval: Duration = 2.seconds,
            enabled: Boolean = Config.isDebug
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

    /**
     * Log the duration of a single task and return the result from that task.
     *
     * TODO: use contracts when they are no longer experimental
     */
    fun <T> measure(taskName: String? = null, task: () -> T): T {
        // contract { callsInPlace(task, EXACTLY_ONCE) }
        return runBlocking { measureSuspend(taskName) { task() } }
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
    private var updateClock = Clock.markNow()

    // TODO: use contracts when they are no longer experimental
    override suspend fun <T> measureSuspend(taskName: String?, task: suspend () -> T): T {
        // contract { callsInPlace(task, EXACTLY_ONCE) }
        val (duration, result) = measureTime { task() }

        executionCount++
        executionTotalDuration += duration

        if (updateClock.elapsedSince() > updateInterval) {
            updateClock = Clock.markNow()
            Log.d(
                tag,
                "$name${if (!taskName.isNullOrEmpty()) ".$taskName" else ""} executing on " +
                    "thread ${Thread.currentThread().name} " +
                    "AT ${executionCount / executionTotalDuration.inSeconds} FPS, " +
                    "${executionTotalDuration.inMilliseconds / executionCount} MS/F"
            )
        }
        return result
    }
}
