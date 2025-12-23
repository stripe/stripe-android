package com.stripe.android.paymentsheet.example.playground.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateConnectionTokenRequest(
    @SerialName("merchant_country_code")
    val merchantCountryCode: String,
    @SerialName("custom_stripe_api")
    val customStripeApi: String?,
    @SerialName("custom_secret_key")
    val customSecretKey: String?,
    @SerialName("custom_publishable_key")
    val customPublishableKey: String?,
)

@Serializable
data class CreateConnectionTokenResponse(
    @SerialName("connection_token")
    val connectionToken: String,
)
