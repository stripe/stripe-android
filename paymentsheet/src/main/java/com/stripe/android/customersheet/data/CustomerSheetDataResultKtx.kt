package com.stripe.android.customersheet.data

import com.stripe.android.core.exception.StripeException
import com.stripe.android.customersheet.CustomerAdapter

internal fun <T> CustomerAdapter.Result<T>.toCustomerSheetDataResult(): CustomerSheetDataResult<T> {
    return when (this) {
        is CustomerAdapter.Result.Success -> CustomerSheetDataResult.success(value)
        is CustomerAdapter.Result.Failure -> CustomerSheetDataResult.failure(
            cause = cause,
            displayMessage = displayMessage,
        )
    }
}

internal fun <T> Result<T>.toCustomerSheetDataResult(): CustomerSheetDataResult<T> {
    return fold(
        onSuccess = {
            CustomerSheetDataResult.success(it)
        },
        onFailure = {
            CustomerSheetDataResult.failure(cause = it, displayMessage = null)
        },
    )
}

internal inline fun <R, T> CustomerSheetDataResult<T>.map(
    transform: (value: T) -> R
): CustomerSheetDataResult<R> {
    return when (this) {
        is CustomerSheetDataResult.Success -> CustomerSheetDataResult.success(transform(this.value))
        is CustomerSheetDataResult.Failure -> CustomerSheetDataResult.failure(
            cause = this.cause,
            displayMessage = this.displayMessage
        )
    }
}

internal inline fun <R, T> CustomerSheetDataResult<T>.mapCatching(
    transform: (value: T) -> R
): CustomerSheetDataResult<R> {
    return when (this) {
        is CustomerSheetDataResult.Success -> runCatching { transform(this.value) }
        is CustomerSheetDataResult.Failure -> CustomerSheetDataResult.failure(
            cause = this.cause,
            displayMessage = this.displayMessage
        )
    }
}

internal inline fun <R, T> CustomerSheetDataResult<T>.onSuccess(
    action: (value: T) -> R
): CustomerSheetDataResult<T> {
    if (this is CustomerSheetDataResult.Success) {
        action(this.value)
    }
    return this
}

internal inline fun <R, T> CustomerSheetDataResult<T>.onFailure(
    action: (cause: Throwable, displayMessage: String?) -> R
): CustomerSheetDataResult<T> {
    failureOrNull()?.let {
        val displayMessage = it.displayMessage
            ?: (it.cause as? StripeException)?.stripeError?.message
        action(it.cause, displayMessage)
    }
    return this
}

internal inline fun <R, T> CustomerSheetDataResult<T>.fold(
    onSuccess: (value: T) -> R,
    onFailure: (cause: Throwable, displayMessage: String?) -> R
): R {
    return when (this) {
        is CustomerSheetDataResult.Failure -> {
            onFailure(this.cause, this.displayMessage)
        }
        is CustomerSheetDataResult.Success -> {
            onSuccess(this.value)
        }
    }
}

internal fun<T> CustomerSheetDataResult<T>.failureOrNull(): CustomerSheetDataResult.Failure<T>? =
    when (this) {
        is CustomerSheetDataResult.Failure -> this
        else -> null
    }

private inline fun <R, T> T.runCatching(block: T.() -> R): CustomerSheetDataResult<R> {
    return kotlin.runCatching {
        CustomerSheetDataResult.success(block())
    }.fold(
        onSuccess = { it },
        onFailure = { CustomerSheetDataResult.failure(it, null) }
    )
}
