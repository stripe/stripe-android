package com.stripe.android.customersheet

import com.stripe.android.core.exception.StripeException

@OptIn(ExperimentalCustomerSheetApi::class)
internal fun <T> CustomerAdapter.Result<T>.getOrNull(): T? =
    when (this) {
        is CustomerAdapter.Result.Failure -> null
        is CustomerAdapter.Result.Success -> this.value
    }

@OptIn(ExperimentalCustomerSheetApi::class)
internal inline infix fun <R, T> CustomerAdapter.Result<T>.flatMap(
    transform: (T) -> CustomerAdapter.Result<R>
): CustomerAdapter.Result<R> {
    return when (this) {
        is CustomerAdapter.Result.Success -> transform(this.value)
        is CustomerAdapter.Result.Failure -> CustomerAdapter.Result.failure(
            cause = this.cause,
            displayMessage = this.displayMessage
        )
    }
}

@OptIn(ExperimentalCustomerSheetApi::class)
internal inline fun <R, T> CustomerAdapter.Result<T>.map(
    transform: (value: T) -> R
): CustomerAdapter.Result<R> {
    return when (this) {
        is CustomerAdapter.Result.Success -> CustomerAdapter.Result.success(transform(this.value))
        is CustomerAdapter.Result.Failure -> CustomerAdapter.Result.failure(
            cause = this.cause,
            displayMessage = this.displayMessage
        )
    }
}

@OptIn(ExperimentalCustomerSheetApi::class)
internal inline fun <R, T> CustomerAdapter.Result<T>.mapCatching(
    transform: (value: T) -> R
): CustomerAdapter.Result<R> {
    return when (this) {
        is CustomerAdapter.Result.Success -> runCatching { transform(this.value) }
        is CustomerAdapter.Result.Failure -> CustomerAdapter.Result.failure(
            cause = this.cause,
            displayMessage = this.displayMessage
        )
    }
}

@OptIn(ExperimentalCustomerSheetApi::class)
@Suppress("TooGenericExceptionCaught")
private inline fun <R, T> T.runCatching(block: T.() -> R): CustomerAdapter.Result<R> {
    return try {
        CustomerAdapter.Result.success(block())
    } catch (e: Throwable) {
        CustomerAdapter.Result.failure(e, null)
    }
}

@OptIn(ExperimentalCustomerSheetApi::class)
internal inline fun <R, T> CustomerAdapter.Result<T>.fold(
    onSuccess: (value: T) -> R,
    onFailure: (cause: Throwable, displayMessage: String?) -> R
): R {
    return when (this) {
        is CustomerAdapter.Result.Failure -> {
            onFailure(this.cause, this.displayMessage)
        }
        is CustomerAdapter.Result.Success -> {
            onSuccess(this.value)
        }
    }
}

@OptIn(ExperimentalCustomerSheetApi::class)
internal inline fun <R, T> CustomerAdapter.Result<T>.onSuccess(
    action: (value: T) -> R
): CustomerAdapter.Result<T> {
    if (this is CustomerAdapter.Result.Success) {
        action(this.value)
    }
    return this
}

@OptIn(ExperimentalCustomerSheetApi::class)
internal inline fun <R, T> CustomerAdapter.Result<T>.onFailure(
    action: (cause: Throwable, displayMessage: String?) -> R
): CustomerAdapter.Result<T> {
    failureOrNull()?.let {
        val displayMessage = it.displayMessage
            ?: (it.cause as? StripeException)?.stripeError?.message
        action(it.cause, displayMessage)
    }
    return this
}

@OptIn(ExperimentalCustomerSheetApi::class)
internal fun<T> CustomerAdapter.Result<T>.failureOrNull(): CustomerAdapter.Result.Failure<T>? =
    when (this) {
        is CustomerAdapter.Result.Failure -> this
        else -> null
    }
