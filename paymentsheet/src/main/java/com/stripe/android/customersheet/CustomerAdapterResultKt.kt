@file:Suppress("UNCHECKED_CAST")

package com.stripe.android.customersheet

import com.stripe.android.core.exception.StripeException

@OptIn(ExperimentalCustomerSheetApi::class)
internal fun <T> CustomerAdapter.Result<T>.getOrNull(): T? =
    when {
        isFailure -> null
        else -> value as T
    }

@OptIn(ExperimentalCustomerSheetApi::class)
internal inline infix fun <R, T> CustomerAdapter.Result<T>.flatMap(
    transform: (T) -> CustomerAdapter.Result<R>
): CustomerAdapter.Result<R> {
    return when {
        isSuccess -> transform(value as T)
        else -> CustomerAdapter.Result(value)
    }
}

@OptIn(ExperimentalCustomerSheetApi::class)
internal inline fun <R, T> CustomerAdapter.Result<T>.map(
    transform: (value: T) -> R
): CustomerAdapter.Result<R> {
    return when {
        isSuccess -> CustomerAdapter.Result.success(transform(value as T))
        else -> CustomerAdapter.Result(value)
    }
}

@OptIn(ExperimentalCustomerSheetApi::class)
internal inline fun <R, T> CustomerAdapter.Result<T>.mapCatching(
    transform: (value: T) -> R
): CustomerAdapter.Result<R> {
    return when {
        isSuccess -> runCatching { transform(value as T) }
        else -> CustomerAdapter.Result(value)
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
    return when (val failure = value as? CustomerAdapter.Result.Failure) {
        is CustomerAdapter.Result.Failure -> onFailure(failure.cause, failure.displayMessage)
        else -> onSuccess(value as T)
    }
}

@OptIn(ExperimentalCustomerSheetApi::class)
internal inline fun <R, T> CustomerAdapter.Result<T>.onSuccess(
    action: (value: T) -> R
): CustomerAdapter.Result<T> {
    if (isSuccess) action(value as T)
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
internal fun<T> CustomerAdapter.Result<T>.failureOrNull(): CustomerAdapter.Result.Failure? =
    when {
        isFailure -> value as CustomerAdapter.Result.Failure
        else -> null
    }
