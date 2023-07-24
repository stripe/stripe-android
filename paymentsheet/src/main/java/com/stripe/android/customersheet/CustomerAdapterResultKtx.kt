@file:Suppress("UNCHECKED_CAST")
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.customersheet

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException

/**
 * Returns the encapsulated value if this instance represents success or null if it is failure.
 */
@ExperimentalCustomerSheetApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> CustomerAdapter.Result<T>.getOrNull(): T? =
    when {
        isFailure -> null
        else -> value as T
    }

/**
 * Returns the encapsulated [CustomerAdapter.Result.Failure] if this instance represents failure or
 * null if it is success.
 */
@ExperimentalCustomerSheetApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> CustomerAdapter.Result<T>.failureOrNull(): CustomerAdapter.Result.Failure? =
    when {
        isFailure -> value as CustomerAdapter.Result.Failure
        else -> null
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
