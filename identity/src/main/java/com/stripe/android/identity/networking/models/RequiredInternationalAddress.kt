package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class RequiredInternationalAddress(
    @SerialName("line1")
    val line1: String,
    @SerialName("line2")
    val line2: String? = null,
    @SerialName("city")
    val city: String,
    @SerialName("postal_code")
    val postalCode: String,
    @SerialName("state")
    val state: String?,
    @SerialName("country")
    val country: String
) : Parcelable
