package com.stripe.tta.demo.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSetupIntentRequest(
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("merchant_country_code")
    val merchantCountryCode: String,
    @SerialName("payment_method_types")
    val paymentMethodTypes: List<String>,
)

@Serializable
data class CreateSetupIntentResponse(
    @SerialName("client_secret")
    val clientSecret: String,
)
