package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateLinkAuthTokenResponse(
    @SerialName("link_auth_token_client_secret")
    val linkAuthTokenClientSecret: String,

    @SerialName("expires_in")
    val expiresIn: Int,
)
