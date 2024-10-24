package com.stripe.android.utils

import com.stripe.android.common.coroutines.Single
import kotlinx.coroutines.CompletableDeferred

internal class CompletableSingle<T>(val value: T? = null) : Single<T> {
    private val deferred: CompletableDeferred<T> = value?.let {
        CompletableDeferred(it)
    } ?: CompletableDeferred()

    override suspend fun await(): T {
        return deferred.await()
    }

    fun complete(value: T) {
        deferred.complete(value)
    }
}
