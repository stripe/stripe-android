package com.stripe.android.financialconnections.model

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 * @param lastAttemptedAt The time at which the last refresh attempt was initiated.
 * Measured in seconds since the Unix epoch.
 * @param status
 */

@Serializable
@Parcelize
@Suppress("unused")
@Poko
class OwnershipRefresh internal constructor(

    /* The time at which the last refresh attempt was initiated. Measured in seconds since the Unix epoch. */
    @SerialName("last_attempted_at")
    val lastAttemptedAt: Int,

    @SerialName("status")
    val status: Status = Status.UNKNOWN

) : Parcelable {

    @Serializable
    enum class Status(val value: String) {
        @SerialName("failed")
        FAILED("failed"),

        @SerialName("pending")
        PENDING("pending"),

        @SerialName("succeeded")
        SUCCEEDED("succeeded"),

        UNKNOWN("unknown")
    }
}
