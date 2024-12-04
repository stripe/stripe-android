package com.stripe.android.camera.framework

import androidx.annotation.RestrictTo
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Keep track of a single stat's duration and result
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface StatTracker {

    /**
     * When this task was started.
     */
    val startedAt: ComparableTimeMark

    /**
     * Track the result from a stat.
     */
    suspend fun trackResult(result: String? = null)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StatTrackerImpl(
    private val onComplete: suspend (ComparableTimeMark, String?) -> Unit
) : StatTracker {
    override val startedAt = TimeSource.Monotonic.markNow()
    override suspend fun trackResult(result: String?) = onComplete(startedAt, result)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class TaskStats(
    val started: ComparableTimeMark,
    val duration: Duration,
    val result: String?
)
