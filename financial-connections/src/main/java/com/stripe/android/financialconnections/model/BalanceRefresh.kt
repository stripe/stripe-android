package com.stripe.android.financialconnections.model

import android.os.Parcelable
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Suppress("unused")
data class BalanceRefresh(
    @SerialName("status") val status: BalanceRefreshStatus? = BalanceRefreshStatus.UNKNOWN,
    @SerialName("last_attempted_at") val lastAttemptedAt: Int
) : StripeModel, Parcelable {

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
