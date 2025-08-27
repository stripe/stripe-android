package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateAuthIntentRequest(
    val email: String,

    @SerialName("oauth_scopes")
    val oauthScopes: String
)
