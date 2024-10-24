package com.stripe.android.common.coroutines

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

internal fun interface Single<T> {
    suspend fun await(): T
}

internal fun <T> StateFlow<T?>.asSingle(): Single<T> {
    return Single {
        value ?: filterNotNull().first()
    }
}
