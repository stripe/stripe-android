package com.stripe.android.paymentsheet.example.playground.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateConnectionTokenRequest(
    @SerialName("merchant_country_code")
    val merchantCountryCode: String,
)

@Serializable
data class CreateConnectionTokenResponse(
    @SerialName("connection_token")
    val connectionToken: String,
)
