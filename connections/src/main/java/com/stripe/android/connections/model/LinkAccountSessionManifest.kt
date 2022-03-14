package com.stripe.android.connections.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkAccountSessionManifest(
    @SerialName("hosted_auth_url") val hostedAuthUrl: String,
    @SerialName("success_url") val successUrl: String,
    @SerialName("cancel_url") val cancelUrl: String
)
