package com.stripe.android.paymentsheet.example.playground.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTerminalSessionRequest(
    @SerialName("merchant_country_code")
    val merchantCountryCode: String,
)

@Serializable
data class CreateTerminalSessionResponse(
    @SerialName("connection_token")
    val connectionToken: String,
    @SerialName("location_id")
    val locationId: String,
)
