package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class NameParam(
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null
) : Parcelable
