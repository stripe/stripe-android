package com.stripe.android.common.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/*
 * This is a helper for catching thrown exceptions from 'async' calls within a suspendable function.
 *
 * If a 'runCatching' task wraps a task with 'async' calls, any 'async' failures are not caught by 'runCatching'
 * but instead by the coroutine, causing the whole coroutine scope to fail and close which propagates the uncaught
 * exception to the caller. This is counter-intuitive to the idea of catching all exceptions that occur from a
 * task.
 *
 * This function instead launches inside one coroutine scope a 'runCatching' call that creates its own coroutine
 * scope to launch the task in. If the inner coroutine scope fails and throws an exception, the 'runCatching' call
 * will be able to catch the exception and return a 'Result' type to the caller rather than throw an exception to
 * it. If a failure occurs, we can also run the 'onFailure' logic inside the outer work coroutine rather than the
 * caller's coroutine.
 */
internal suspend fun <T> CoroutineContext.runCatching(
    onFailure: ((error: Throwable) -> Unit)? = null,
    task: suspend CoroutineScope.() -> T
): Result<T> {
    return withContext(this) {
        runCatching {
            coroutineScope {
                task()
            }
        }.onFailure {
            onFailure?.invoke(it)
        }
    }
}
