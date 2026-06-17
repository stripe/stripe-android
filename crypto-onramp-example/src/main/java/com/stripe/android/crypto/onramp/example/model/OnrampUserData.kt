package com.stripe.android.crypto.onramp.example.model

import kotlinx.serialization.Serializable

@Serializable
internal data class OnrampUserData(
    val email: String,
    val authToken: String,
    val cryptoCustomerId: String? = null,
)
