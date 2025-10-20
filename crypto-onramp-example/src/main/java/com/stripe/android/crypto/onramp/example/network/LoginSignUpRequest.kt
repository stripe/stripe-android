package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginSignUpRequest(
    val email: String,
    val password: String,
    val livemode: Boolean,
)
