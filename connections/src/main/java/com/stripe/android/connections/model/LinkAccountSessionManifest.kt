package com.stripe.android.connections.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class LinkAccountSessionManifest(
    @SerialName("hosted_auth_url") val hostedAuthUrl: String,
    @SerialName("success_url") val successUrl: String,
    @SerialName("cancel_url") val cancelUrl: String
) : Parcelable
