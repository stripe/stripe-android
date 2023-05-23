package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class PhoneParam(
    @SerialName("country_code")
    val countryCode: String? = null,
    @SerialName("number")
    val number: String? = null
) : Parcelable
