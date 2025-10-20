package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaveUserRequest(
    @SerialName("crypto_customer_id")
    val cryptoCustomerId: String,
)
