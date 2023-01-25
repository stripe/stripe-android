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
) : Parcelable
