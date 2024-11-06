package com.stripe.android.connect.example.networking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchClientSecretResponse(
    @SerialName("client_secret")
    val clientSecret: String
)
