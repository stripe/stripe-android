package com.stripe.android.connect.example.ui.common

/**
 * Represents an asynchronous operation that is either:
 * 1. uninitialized (not started)
 * 2. loading
 * 3. successful (finished)
 * 4. failed (finished)
 *
 * Taken with modification from airbnb's Mavericks library:
 * https://github.com/airbnb/mavericks/blob/e920c4b0fe73183a3bb51e648066ebd5f7be3e8c/mvrx-common/src/main/java/com/airbnb/mvrx/Async.kt#L10
 */
sealed class Async<out T>(val complete: Boolean, val shouldLoad: Boolean, private val value: T?) {
    /**
     * Returns the value or null.
     */
    open operator fun invoke(): T? = value
}

data object Uninitialized : Async<Nothing>(complete = false, shouldLoad = true, value = null)

data class Loading<out T>(private val value: T? = null) : Async<T>(complete = false, shouldLoad = false, value = value)

data class Success<out T>(private val value: T) : Async<T>(complete = true, shouldLoad = false, value = value)

data class Fail<out T>(
    val error: Throwable,
    private val value: T? = null,
) : Async<T>(complete = true, shouldLoad = true, value = value) {
    override fun equals(other: Any?): Boolean {
        if (other !is Fail<*>) return false

        val otherError = other.error
        return error::class == otherError::class &&
            error.message == otherError.message &&
            error.stackTrace.firstOrNull() == otherError.stackTrace.firstOrNull()
    }

    override fun hashCode(): Int = arrayOf(error::class, error.message, error.stackTrace.firstOrNull()).contentHashCode()
}
