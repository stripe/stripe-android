package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class PhoneParam(
    @SerialName("country")
    val country: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null
) : Parcelable
