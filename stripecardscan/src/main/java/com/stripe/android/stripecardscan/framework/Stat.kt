package com.stripe.android.stripecardscan.framework

import android.util.Log
import androidx.annotation.CheckResult
import com.stripe.android.stripecardscan.framework.time.Clock
import com.stripe.android.stripecardscan.framework.time.ClockMark
import com.stripe.android.stripecardscan.framework.time.Duration
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

internal object Stats {
    val instanceId = UUID.randomUUID().toString()

    var scanId: String? = null
        private set

    private var persistentRepeatingTasks:
        MutableMap<String, MutableMap<String, RepeatingTaskStats>> = mutableMapOf()
    private var tasks: MutableMap<String, List<TaskStats>> = mutableMapOf()
    private var repeatingTasks: MutableMap<String, MutableMap<String, RepeatingTaskStats>> =
        mutableMapOf()

    private val scanIdMutex = Mutex()
    private val taskMutex = Mutex()
    private val repeatingTaskMutex = Mutex()
    private val persistentRepeatingTasksMutex = Mutex()

    fun startScan() {
        runBlocking {
            scanIdMutex.withLock {
                clearAllTasks()
                scanId = UUID.randomUUID().toString()
            }
        }
    }

    /**
     * Track the duration of a task.
     */
    @CheckResult
    fun trackTask(name: String): StatTracker =
        StatTrackerImpl { startedAt, result ->
            taskMutex.withLock {
                val list = tasks[name]
                if (list == null) {
                    tasks[name] = listOf(TaskStats(startedAt, startedAt.elapsedSince(), result))
                } else {
                    tasks[name] = list + TaskStats(startedAt, startedAt.elapsedSince(), result)
                }
            }
            if (Config.isDebug) {
                Log.v(
                    Config.logTag,
                    "Task $name got result $result after ${startedAt.elapsedSince()}",
                )
            }
        }

    /**
     * Track the result of a task.
     */
    suspend fun <T> trackTask(name: String, task: suspend () -> T): T {
        val tracker = trackTask(name)
        val result: T
        try {
            result = task()
            tracker.trackResult("success")
        } catch (t: Throwable) {
            tracker.trackResult(t::class.java.simpleName)
            throw t
        }

        return result
    }

    /**
     * Track a single execution of a repeating task.
     */
    @CheckResult
    fun trackRepeatingTask(name: String): StatTracker =
        StatTrackerImpl { startedAt, result ->
            repeatingTaskMutex.withLock {
                val resultName = result ?: "null"
                val resultStats = repeatingTasks[name] ?: run {
                    val taskStats = mutableMapOf<String, RepeatingTaskStats>()
                    repeatingTasks[name] = taskStats
                    taskStats
                }

                val taskStats = resultStats[resultName]
                val duration = startedAt.elapsedSince()
                if (taskStats == null) {
                    resultStats[resultName] = RepeatingTaskStats(
                        executions = 1,
                        startedAt = startedAt,
                        totalDuration = duration,
                        totalCpuDuration = duration,
                        minimumDuration = duration,
                        maximumDuration = duration,
                    )
                } else {
                    resultStats[resultName] = RepeatingTaskStats(
                        executions = taskStats.executions + 1,
                        startedAt = taskStats.startedAt,
                        totalDuration = taskStats.startedAt.elapsedSince(),
                        totalCpuDuration = taskStats.totalCpuDuration + duration,
                        minimumDuration = minOf(taskStats.minimumDuration, duration),
                        maximumDuration = maxOf(taskStats.maximumDuration, duration),
                    )
                }
            }
            if (Config.isDebug) {
                Log.v(
                    Config.logTag,
                    "Repeating task $name got result $result after ${startedAt.elapsedSince()}",
                )
            }
        }

    /**
     * Track a single execution of a repeating task that should not be cleared on scan start.
     */
    @CheckResult
    fun trackPersistentRepeatingTask(name: String): StatTracker =
        StatTrackerImpl { startedAt, result ->
            persistentRepeatingTasksMutex.withLock {
                val resultName = result ?: "null"
                val resultStats = persistentRepeatingTasks[name] ?: run {
                    val taskStats = mutableMapOf<String, RepeatingTaskStats>()
                    persistentRepeatingTasks[name] = taskStats
                    taskStats
                }

                val taskStats = resultStats[resultName]
                val duration = startedAt.elapsedSince()
                if (taskStats == null) {
                    resultStats[resultName] = RepeatingTaskStats(
                        executions = 1,
                        startedAt = startedAt,
                        totalDuration = duration,
                        totalCpuDuration = duration,
                        minimumDuration = duration,
                        maximumDuration = duration,
                    )
                } else {
                    resultStats[resultName] = RepeatingTaskStats(
                        executions = taskStats.executions + 1,
                        startedAt = taskStats.startedAt,
                        totalDuration = taskStats.startedAt.elapsedSince(),
                        totalCpuDuration = taskStats.totalCpuDuration + duration,
                        minimumDuration = minOf(taskStats.minimumDuration, duration),
                        maximumDuration = maxOf(taskStats.maximumDuration, duration),
                    )
                }
            }
            if (Config.isDebug) {
                Log.v(
                    Config.logTag,
                    "Persistent repeating task $name got result $result after " +
                        "${startedAt.elapsedSince()}",
                )
            }
        }

    /**
     * Track the result of a task.
     */
    suspend fun <T> trackRepeatingTask(name: String, task: () -> T): T {
        val tracker = trackRepeatingTask(name)
        val result: T
        try {
            result = task()
            tracker.trackResult("success")
        } catch (t: Throwable) {
            tracker.trackResult(t::class.java.simpleName)
            throw t
        }

        return result
    }

    @JvmStatic
    @CheckResult
    fun getRepeatingTasks() = runBlocking {
        repeatingTaskMutex.withLock {
            persistentRepeatingTasksMutex.withLock {
                repeatingTasks.toMap().mapValues { entry -> entry.value.toMap() } +
                    persistentRepeatingTasks.toMap().mapValues { entry -> entry.value.toMap() }
            }
        }
    }

    @JvmStatic
    @CheckResult
    fun getTasks() = runBlocking {
        taskMutex.withLock { tasks.toMap() }
    }

    private suspend fun clearAllTasks() = supervisorScope {
        val tasksAsync = async { taskMutex.withLock { tasks.clear() } }
        val repeatingTasksAsync = async { repeatingTaskMutex.withLock { repeatingTasks.clear() } }

        tasksAsync.await()
        repeatingTasksAsync.await()
    }
}

/**
 * Keep track of a single stat's duration and result
 */
internal interface StatTracker {

    /**
     * When this task was started.
     */
    val startedAt: ClockMark

    /**
     * Track the result from a stat.
     */
    suspend fun trackResult(result: String? = null)
}

private class StatTrackerImpl(
    private val onComplete: suspend (ClockMark, String?) -> Unit,
) : StatTracker {
    override val startedAt = Clock.markNow()
    override suspend fun trackResult(result: String?) =
        coroutineScope { launch { onComplete(startedAt, result) } }.let { }
}

internal data class TaskStats(
    val started: ClockMark,
    val duration: Duration,
    val result: String?,
)

internal data class RepeatingTaskStats(
    val executions: Int,
    val startedAt: ClockMark,
    val totalDuration: Duration,
    val totalCpuDuration: Duration,
    val minimumDuration: Duration,
    val maximumDuration: Duration,
) {
    fun averageDuration() = totalCpuDuration / executions
}
