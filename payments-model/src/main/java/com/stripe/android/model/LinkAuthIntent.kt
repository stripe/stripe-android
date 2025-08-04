package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class LinkAuthIntent(
    val id: String,
    val state: LinkAuthIntentState,
) : StripeModel

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class LinkAuthIntentState(val value: String) : Parcelable {
    Unknown(""),
    Created("created"),
    Authenticated("authenticated"),
    Consented("consented");

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun fromValue(value: String): LinkAuthIntentState =
            LinkAuthIntentState.entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: Unknown
    }
}
