package com.stripe.attestation

import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/**
 * Converts a *finished* [Task] to a [Result].
 */
fun <T> Task<T>.toResult(): Result<T> {
    return when {
        isSuccessful -> Result.success(result)
        isCanceled -> Result.failure(CancellationException("Task was canceled"))
        else -> Result.failure(exception ?: Exception("Unknown error occurred"))
    }
}

/**
 * Awaits the completion of the [Task] and returns the result.
 *
 * @param cancellationTokenSource A [CancellationTokenSource] that can be used to cancel the task.
 * @return The result of the [Task]. At this point, the task has been executed and is complete.
 */
internal suspend fun <T> Task<T>.awaitTask(cancellationTokenSource: CancellationTokenSource? = null): Task<T> {
    return if (isComplete) {
        this
    } else {
        suspendCancellableCoroutine { cont ->
            // Run the callback directly to avoid unnecessarily scheduling on the main thread.
            addOnCompleteListener(DirectExecutor, cont::resume)

            cancellationTokenSource?.let { cancellationSource ->
                cont.invokeOnCancellation { cancellationSource.cancel() }
            }
        }
    }
}

/**
 * An [Executor] that just directly executes the [Runnable].
 */
private object DirectExecutor : Executor {
    override fun execute(r: Runnable) {
        r.run()
    }
}
