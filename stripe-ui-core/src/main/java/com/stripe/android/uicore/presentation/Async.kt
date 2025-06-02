package com.stripe.android.uicore.presentation

import androidx.annotation.RestrictTo

/**
 * Represents the state of an asynchronous operation.
 *
 * @param T The type of the value that the operation will produce.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class Async<out T>(
    private val value: T?
) {

    /**
     * Represents the initial state before any async operation has been started.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Uninitialized : Async<Nothing>(value = null)

    /**
     * Represents an ongoing async operation.
     *
     * @param T The type of the data being loaded
     * @param value An optional previous or intermediate value to display during loading
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Loading<T>(val value: T? = null) : Async<T>(value = value)

    /**
     * Represents a successfully completed async operation.
     *
     * @param T The type of the result data
     * @param value The actual result value from the operation
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Success<out T>(private val value: T) : Async<T>(value = value) {
        override operator fun invoke(): T = value
    }

    /**
     * Represents a failed async operation.
     *
     * @param T The type of the result that would have been returned on success
     * @param error The throwable that caused the operation to fail
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Fail<out T>(val error: Throwable) : Async<T>(value = null)

    /**
     * Convenience operator to access the wrapped value.
     *
     * This allows using an Async instance directly as a function to retrieve its value.
     * For example: `val data = myAsync()` instead of `val data = myAsync.value`.
     *
     * @return The wrapped value, which may be null depending on the state
     */
    open operator fun invoke(): T? = value
}
