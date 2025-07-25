package com.stripe.android.paymentsheet.utils

internal inline fun <T, R> Result<T>.flatMapCatching(transform: (T) -> Result<R>): Result<R> {
    return mapCatching { result ->
        transform(result).getOrThrow()
    }
}
