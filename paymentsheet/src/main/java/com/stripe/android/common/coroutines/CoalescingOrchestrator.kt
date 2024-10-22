package com.stripe.android.common.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * This class intends to simplify workflows where calling a factory multiple times in parallel only results in a single
 * call to the underlying factory, but the data returned is shared between all callers.
 */
internal class CoalescingOrchestrator<T : Any>(
    private val factory: suspend () -> T,
    private val keepDataInMemory: (T) -> Boolean = { false },
    // This should only be used for testing.
    private val awaitListener: (() -> Unit)? = null,
) {
    @Volatile
    private lateinit var data: T

    @Volatile
    private var dataInitialized: Boolean = false

    @Volatile
    private var deferred: Deferred<T>? = null
    private val lock: Any = Any()

    tailrec suspend fun get(): T {
        if (dataInitialized) {
            return data
        }

        val result = coroutineScope {
            val deferredToAwait: Deferred<T>
            synchronized(lock) {
                if (dataInitialized) {
                    return@coroutineScope data
                }
                val localDeferred = deferred
                if (localDeferred != null && !localDeferred.isCancelled) {
                    deferredToAwait = localDeferred
                } else {
                    deferredToAwait = loadDataAsync(this@coroutineScope)
                }
            }
            try {
                awaitListener?.invoke()
                deferredToAwait.await()
            } catch (_: CancellationException) {
                // The `deferredToAwait` was cancelled before we could await it by another thread.
                null
            }
        }
        if (result != null) {
            return result
        }
        // Null returned due to cancellation. Try again by calling ourself.
        return get()
    }

    private fun loadDataAsync(scope: CoroutineScope): Deferred<T> {
        val local = scope.async(start = CoroutineStart.LAZY) {
            val result = factory()
            synchronized(lock) {
                if (keepDataInMemory(result)) {
                    data = result
                    dataInitialized = true
                }
                deferred = null
            }
            result
        }

        deferred = local

        return local
    }
}
