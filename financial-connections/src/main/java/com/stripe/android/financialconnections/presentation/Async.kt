package com.stripe.android.financialconnections.presentation

/**
 * Represents the state of an asynchronous operation.
 *
 * @param T The type of the value that the operation will produce.
 */
internal sealed class Async<out T>(
    private val value: T?
) {

    data object Uninitialized : Async<Nothing>(value = null)

    data class Loading<T>(val value: T? = null) : Async<T>(value = value)
    data class Success<out T>(private val value: T) : Async<T>(value = value) {
        override operator fun invoke(): T = value
    }

    data class Fail<out T>(val error: Throwable) : Async<T>(value = null)

    open operator fun invoke(): T? = value
}
