package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateAuthIntentResponse(
    val data: AuthIntentData,
    val token: String
)

@Serializable
data class AuthIntentData(
    val id: String,
    @SerialName("expires_at")
    val expiresAt: Long
)
