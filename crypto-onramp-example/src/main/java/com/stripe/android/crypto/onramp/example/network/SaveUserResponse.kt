package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.Serializable

@Serializable
data class SaveUserResponse(
    val success: Boolean,
)
