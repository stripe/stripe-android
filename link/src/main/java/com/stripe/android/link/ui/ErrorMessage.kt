package com.stripe.android.link.ui

import android.content.res.Resources
import android.os.Parcelable
import androidx.annotation.StringRes
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.link.R
import kotlinx.parcelize.Parcelize

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
internal sealed class ErrorMessage : Parcelable {
    abstract fun getMessage(resources: Resources): String

    @Parcelize
    data class FromResources(
        @StringRes val stringResId: Int
    ) : ErrorMessage() {
        override fun getMessage(resources: Resources) =
            resources.getString(stringResId)
    }

    @Parcelize
    data class Raw(
        val errorMessage: String
    ) : ErrorMessage() {
        override fun getMessage(resources: Resources) = errorMessage
    }
}
