@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <T, R> Result<T>.mapResultCatching(
    transform: (T) -> Result<R>,
): Result<R> {
    return fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) },
    )
}
