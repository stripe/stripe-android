package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthCreateResponse(
    val authIntentId: String,
    val existing: Boolean,
    val state: AuthState,
    val token: String
)

enum class AuthState {
    @SerialName("created")
    CREATED,

    @SerialName("authenticated")
    AUTHENTICATED,

    @SerialName("consented")
    CONSENTED,

    @SerialName("rejected")
    REJECTED,

    @SerialName("expired")
    EXPIRED
}
