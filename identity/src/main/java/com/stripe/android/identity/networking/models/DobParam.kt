package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class DobParam(
    @SerialName("day")
    val day: String? = null,
    @SerialName("month")
    val month: String? = null,
    @SerialName("year")
    val year: String? = null
) : Parcelable {
    override fun toString(): String {
        return "$month/$day/$year"
    }

    companion object {
        fun String.toDob(): DobParam? =
            if (this.matches(regexMMDDYYYY)) {
                DobParam(
                    day = this.substring(2, 4),
                    month = this.substring(0, 2),
                    year = this.substring(4)
                )
            } else {
                null
            }

        val regexMMDDYYYY =
            Regex("(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])(19|20)\\d\\d")
    }
}
