package com.stripe.android.stripecardscan.framework.util

import com.stripe.android.camera.framework.time.Duration
import com.stripe.android.camera.framework.util.UnexpectedRetryException
import kotlinx.coroutines.delay

private const val DEFAULT_RETRIES = 3

/**
 * Call a given [task]. If the task throws an exception not included in the [excluding] list, retry
 * the task up to [times], each time after a delay of [retryDelayFunction].
 *
 * @param retryDelayFunction calculate the amount of time between a failed task and the next retry
 * @param times the number of times to retry the task
 * @param excluding a list of exceptions to fail immediately on
 * @param task the task to retry
 *
 * TODO: use contracts when they're no longer experimental
 */
internal suspend fun <T> retry(
    retryDelayFunction: (attempt: Int, totalAttempts: Int) -> Duration,
    times: Int = DEFAULT_RETRIES,
    excluding: List<Class<out Throwable>> = emptyList(),
    task: suspend () -> T
): T {
//    contract { callsInPlace(task, InvocationKind.AT_LEAST_ONCE) }
    var exception: Throwable? = null
    for (attempt in 1..times) {
        try {
            return task()
        } catch (t: Throwable) {
            exception = t
            if (t.javaClass in excluding) {
                throw t
            }
            if (attempt < times) {
                delay(retryDelayFunction(attempt, times).inMilliseconds.toLong())
            }
        }
    }

    if (exception != null) {
        throw exception
    } else {
        // This code should never be reached
        throw UnexpectedRetryException()
    }
}
