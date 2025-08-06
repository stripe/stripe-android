package com.stripe.android.core.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <T, R> Result<T>.flatMapCatching(transform: (T) -> Result<R>): Result<R> {
    return mapCatching { result ->
        transform(result).getOrThrow()
    }
}
