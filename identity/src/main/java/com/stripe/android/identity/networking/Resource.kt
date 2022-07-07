package com.stripe.android.identity.networking

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
@Parcelize
internal data class Resource<out T>(
    val status: Status,
    val data: @RawValue T?,
    val message: String? = null,
    val throwable: Throwable? = null
) : Parcelable {
    companion object {
        fun <T> success(data: T?): Resource<T> {
            return Resource(Status.SUCCESS, data)
        }

        fun <T> error(
            msg: String? = null,
            throwable: Throwable? = null,
            data: T? = null
        ): Resource<T> {
            return Resource(Status.ERROR, data, msg, throwable)
        }

        fun <T> loading(data: T? = null): Resource<T> {
            return Resource(Status.LOADING, data)
        }
    }
}
