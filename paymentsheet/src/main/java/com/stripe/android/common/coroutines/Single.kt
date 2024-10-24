package com.stripe.android.common.coroutines

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

internal fun interface Single<T> {
    suspend fun await(): T
}

internal suspend fun <T> Single<T>.awaitWithTimeout(
    timeout: Duration,
    timeoutMessage: () -> String,
): Result<T> {
    val result = withTimeoutOrNull(timeout) { await() }
    return if (result != null) {
        Result.success(result)
    } else {
        Result.failure(IllegalStateException(timeoutMessage()))
    }
}

internal fun <T> StateFlow<T?>.asSingle(): Single<T> {
    return Single {
        value ?: filterNotNull().first()
    }
}
