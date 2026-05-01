package com.stripe.android.payments.samsungpay

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * An exception representing a Samsung Pay error.
 *
 * @param errorCode The Samsung Pay SDK error code.
 * @param errorReason Additional error reason code, if available.
 * @param message Human-readable error description.
 */
@Parcelize
class SamsungPayException(
    val errorCode: Int,
    val errorReason: Int? = null,
    override val message: String? = null,
) : Exception(message), Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SamsungPayException) return false
        return errorCode == other.errorCode &&
            errorReason == other.errorReason &&
            message == other.message
    }

    override fun hashCode(): Int {
        var result = errorCode
        result = 31 * result + (errorReason ?: 0)
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }
}
