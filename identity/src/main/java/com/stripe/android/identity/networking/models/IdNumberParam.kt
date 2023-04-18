package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class IdNumberParam(
    @SerialName("country")
    val country: String?,
    @SerialName("partial_value")
    val partialValue: String? = null,
    @SerialName("value")
    val value: String? = null
) : Parcelable
