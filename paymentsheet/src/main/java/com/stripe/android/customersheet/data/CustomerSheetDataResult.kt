package com.stripe.android.customersheet.data

internal sealed interface CustomerSheetDataResult<T> {
    data class Success<T>(val value: T) : CustomerSheetDataResult<T>

    data class Failure<T>(
        val cause: Throwable,
        val displayMessage: String? = null
    ) : CustomerSheetDataResult<T>

    fun toResult(): Result<T> {
        return when (this) {
            is Success -> Result.success(value)
            is Failure -> Result.failure(cause)
        }
    }

    companion object {
        fun <T> success(value: T): Success<T> {
            return Success(value)
        }

        fun <T> failure(cause: Throwable, displayMessage: String?): Failure<T> {
            return Failure(cause = cause, displayMessage = displayMessage)
        }
    }
}
