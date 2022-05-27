package com.stripe.android.link.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.link.R

internal fun Throwable.getErrorMessage() = when (this) {
    is APIConnectionException ->
        ErrorMessage.FromResources(R.string.stripe_failure_connection_error)
    else -> localizedMessage?.let {
        ErrorMessage.Raw(it)
    } ?: ErrorMessage.FromResources(R.string.stripe_internal_error)
}

/**
 * Represents a user-facing error message.
 */
internal sealed class ErrorMessage {
    abstract fun getMessage(resources: Resources): String

    data class FromResources(
        @StringRes val stringResId: Int
    ) : ErrorMessage() {
        override fun getMessage(resources: Resources) =
            resources.getString(stringResId)
    }

    data class Raw(
        val errorMessage: String
    ) : ErrorMessage() {
        override fun getMessage(resources: Resources) = errorMessage
    }
}
