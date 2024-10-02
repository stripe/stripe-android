package com.stripe.android.financialconnections.model

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Suppress("unused")
@Poko
class BalanceRefresh internal constructor(
    @SerialName("status") val status: BalanceRefreshStatus? = BalanceRefreshStatus.UNKNOWN,
    @SerialName("last_attempted_at") val lastAttemptedAt: Int
) : Parcelable {

    @Serializable
    enum class BalanceRefreshStatus(internal val code: String) {
        @SerialName("failed")
        FAILED("failed"),

        @SerialName("pending")
        PENDING("pending"),

        @SerialName("succeeded")
        SUCCEEDED("succeeded"),

        UNKNOWN("unknown")
    }
}
