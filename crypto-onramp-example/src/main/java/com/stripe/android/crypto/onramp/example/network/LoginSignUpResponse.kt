package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginSignUpResponse(
    val success: Boolean,
    val token: String,
    val user: User
)

@Serializable
data class User(
    @SerialName("user_id")
    val id: Int,

    val email: String,

    @SerialName("created_at")
    val createdAt: String
)
