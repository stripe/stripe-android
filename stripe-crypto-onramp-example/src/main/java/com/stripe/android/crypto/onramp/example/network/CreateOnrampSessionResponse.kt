package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateOnrampSessionResponse(
    @SerialName("session_id")
    val sessionId: String? = null,
    val error: String? = null
)
