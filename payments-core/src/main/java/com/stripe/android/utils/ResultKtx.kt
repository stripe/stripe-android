package com.stripe.android.utils

internal inline fun <T, R> Result<T>.mapResult(
    transform: (T) -> Result<R>,
): Result<R> {
    return fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) },
    )
}
