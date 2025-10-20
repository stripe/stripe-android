package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class LinkAuthIntent(
    @SerialName("status")
    val status: Status,
) : StripeModel {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Status(val value: String) {
        Created("created"),
        Authenticated("authenticated"),
        Consented("consented"),
        Rejected("rejected"),
        Expired("expired")
    }
}
