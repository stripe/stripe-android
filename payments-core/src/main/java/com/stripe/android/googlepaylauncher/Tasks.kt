package com.stripe.android.googlepaylauncher

import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume

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
