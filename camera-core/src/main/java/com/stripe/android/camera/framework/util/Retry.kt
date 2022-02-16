package com.stripe.android.camera.framework.util

import androidx.annotation.RestrictTo

private const val DEFAULT_RETRIES = 3

/**
 * Call a given [task]. If the task throws an exception not included in the [excluding] list, retry
 * the task up to [times].
 *
 * @param times the number of times to retry the task
 * @param excluding a list of exceptions to fail immediately on
 * @param task the task to retry
 *
 * TODO: use contracts when they're no longer experimental
 */
internal fun <T> retrySync(
    times: Int = DEFAULT_RETRIES,
    excluding: List<Class<out Throwable>> = emptyList(),
    task: () -> T
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
        }
    }

    if (exception != null) {
        throw exception
    } else {
        // This code should never be reached
        throw UnexpectedRetryException()
    }
}

/**
 * This exception should never be thrown.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class UnexpectedRetryException : Exception()
