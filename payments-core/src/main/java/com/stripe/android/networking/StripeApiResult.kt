package com.stripe.android.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface StripeApiResult<ModelT> {
    data class Success<ModelT>(val model: ModelT) : StripeApiResult<ModelT>
    data class Failure<ModelT>(val error: StripeException) : StripeApiResult<ModelT>
}

internal fun <T> StripeApiResult<T>.getOrNull(): T? {
    return (this as? StripeApiResult.Success)?.model
}

internal fun <InputT, OutputT> StripeApiResult<InputT>.map(
    transform: (InputT) -> OutputT,
): StripeApiResult<OutputT> {
    return when (this) {
        is StripeApiResult.Success -> StripeApiResult.Success(transform(model))
        is StripeApiResult.Failure -> StripeApiResult.Failure(error)
    }
}

internal inline fun <T, R> Result<T>.mapIfSuccess(transform: (T) -> Result<R>): Result<R> {
    return fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) },
    )
}
