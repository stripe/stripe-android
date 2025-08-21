package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateOnrampSessionResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("client_secret")
    val clientSecret: String? = null,
    @SerialName("error")
    val error: String? = null
)
