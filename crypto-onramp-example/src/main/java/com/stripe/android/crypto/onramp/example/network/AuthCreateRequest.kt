package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthCreateRequest(
    @SerialName("oauth_scopes")
    val oauthScopes: String,
)
